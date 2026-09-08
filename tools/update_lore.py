"""Incremental, source-attributed MediaWiki ingestion. Standard library only."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import ssl
import sys
import time
from datetime import datetime, timezone
from html.parser import HTMLParser
from urllib import error, parse, request, robotparser

ROOT = Path(__file__).resolve().parents[1]
AGENT = "ZandikKnowledgeBot/1.0"
MAX_BYTES = 8 * 1024 * 1024

def atomic_write(path, text):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + ".tmp")
    temporary.write_text(text, encoding="utf-8")
    os.replace(temporary, path)

class ArticleText(HTMLParser):
    """Retain article text and paragraph boundaries, discard chrome/scripts/comments."""
    VOID = {"br", "hr", "img", "input", "meta", "link", "wbr", "source", "area", "col", "embed"}
    BLOCK = {"p", "div", "li", "tr", "h1", "h2", "h3", "h4", "blockquote", "section", "dl", "dt", "dd"}
    def __init__(self, fragment=False):
        super().__init__(convert_charrefs=True)
        self.fragment = fragment
        self.stack = []
        self.parts = []
    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        parent_active, parent_skip = self.stack[-1][1:] if self.stack else (self.fragment, False)
        classes = set(attrs.get("class", "").split())
        active = parent_active or "mw-parser-output" in classes or tag == "article"
        skip = parent_skip or tag in {"script", "style", "nav", "footer", "aside", "sup"}
        skip |= bool(classes & {"navbox", "toc", "mw-editsection", "reference", "references",
                                 "portable-infobox", "catlinks", "noprint"})
        skip |= attrs.get("id", "") in {"articleComments", "toc", "comments"}
        if active and not skip and (tag in self.BLOCK or tag == "br"):
            self.parts.append("\n\n")
        if tag not in self.VOID:
            self.stack.append((tag, active, skip))
    def handle_endtag(self, tag):
        for i in range(len(self.stack) - 1, -1, -1):
            if self.stack[i][0] == tag:
                _, active, skip = self.stack[i]
                del self.stack[i:]
                if active and not skip and tag in self.BLOCK:
                    self.parts.append("\n\n")
                break
    def handle_data(self, data):
        active, skip = self.stack[-1][1:] if self.stack else (self.fragment, False)
        if active and not skip:
            self.parts.append(data)
    def text(self):
        paragraphs = []
        for part in re.split(r"\n\s*\n", "".join(self.parts)):
            part = re.sub(r"\s+", " ", part).strip()
            if part.startswith(('首页 >', '如果是第一次来，', '按右上角“WIKI功能', '请上传文件')):
                continue
            if part:
                paragraphs.append(part)
        return "\n\n".join(paragraphs)

def extract_article(html, fragment=False):
    parser = ArticleText(fragment)
    parser.feed(html)
    return parser.text()

def digest(text):
    return hashlib.sha256(text.encode("utf-8")).hexdigest()

def publish(root, key, title, text, metadata, state, analysis=False):
    relative = Path("analysis/imported" if analysis else "sources/web") / (digest(key)[:20] + ".txt")
    target = root / "character" / relative
    content_hash = digest(text)
    previous = state.get(key, {})
    if (previous.get("hash") == content_hash and target.exists()
            and previous.get("file_hash") == digest(target.read_text(encoding="utf-8"))):
        # Repair deleted/edited mirror without rewriting source or invalidating the index.
        expected = target.read_text(encoding="utf-8")
        mirror = root / "dist/character" / relative
        if not mirror.exists() or mirror.read_text(encoding="utf-8") != expected:
            atomic_write(mirror, expected)
        return False
    if target.exists():
        archive = root / "knowledge/archive" / (digest(target.read_text(encoding="utf-8")) + ".txt")
        if not archive.exists():
            atomic_write(archive, target.read_text(encoding="utf-8"))
    header = "\n".join(f"- {k}: {v}" for k, v in metadata.items())
    content = f"# {title}\n{header}\n\n{text}\n"
    atomic_write(target, content)
    atomic_write(root / "dist/character" / relative, content)
    state[key] = {"hash": content_hash, "file_hash": digest(content),
                  "file": str(relative), "metadata": metadata}
    return True

class Fetcher:
    def __init__(self, proxy="system", timeout=25, interval=2):
        proxies = None if proxy == "system" else {} if proxy == "direct" else {"https": proxy, "http": proxy}
        self.opener = request.build_opener(request.ProxyHandler(proxies),
                                          request.HTTPSHandler(context=ssl.create_default_context()))
        self.timeout = timeout
        self.interval = interval
        self.last_request = 0.0
        self.robots = {}
    def read(self, url):
        if parse.urlsplit(url).scheme != "https":
            raise ValueError("Only HTTPS source URLs are supported")
        for attempt in range(3):
            time.sleep(max(0, self.interval - (time.monotonic() - self.last_request)))
            self.last_request = time.monotonic()
            try:
                req = request.Request(url, headers={"User-Agent": AGENT, "Accept": "application/json,text/html,text/plain"})
                with self.opener.open(req, timeout=self.timeout) as response:
                    data = response.read(MAX_BYTES + 1)
                    if len(data) > MAX_BYTES:
                        raise ValueError("Response exceeds 8 MiB")
                    return data.decode(response.headers.get_content_charset() or "utf-8", errors="strict")
            except error.HTTPError as exc:
                if exc.code not in {429, 500, 502, 503, 504} or attempt == 2:
                    raise
                time.sleep(min(10, 2 ** (attempt + 1)))
            except (error.URLError, TimeoutError):
                if attempt == 2:
                    raise
                time.sleep(2 ** attempt)
        raise RuntimeError("No response")
    def check_robots(self, url):
        parts = parse.urlsplit(url)
        origin = f"{parts.scheme}://{parts.netloc}"
        if origin not in self.robots:
            rules = robotparser.RobotFileParser()
            try:
                rules.parse(self.read(origin + "/robots.txt").splitlines())
            except error.HTTPError as exc:
                if exc.code == 404:
                    rules.parse(["User-agent: *", "Allow: /"])
                else:
                    raise RuntimeError(f"Cannot verify robots.txt: HTTP {exc.code}") from exc
            self.robots[origin] = rules
        rules = self.robots[origin]
        if not rules.can_fetch(AGENT, url):
            raise RuntimeError("Site robots.txt disallows this request")
        self.interval = max(self.interval, rules.crawl_delay(AGENT) or 0)
    def page(self, site, title):
        page_url = site["page_url"].format(title=parse.quote(title.replace(" ", "_"), safe="/"))
        api_url = site["api"] + "?" + parse.urlencode({
            "action": "parse", "page": title, "prop": "text|revid",
            "redirects": 1, "format": "json", "formatversion": 2,
        })
        self.check_robots(page_url)
        self.check_robots(api_url)
        data = json.loads(self.read(api_url))
        if "error" in data:
            raise RuntimeError(str(data["error"].get("info", data["error"])))
        parsed = data["parse"]
        html = parsed["text"]
        if isinstance(html, dict):
            html = html["*"]
        text = extract_article(html, fragment=True)
        if len(text) < 120 or any(marker in text.lower() for marker in
                                  ["verify you are human", "just a moment...", "access denied"]):
            raise ValueError("No usable article body; nothing imported")
        return page_url, parsed.get("title", title), text, parsed.get("revid")

def main(argv=None):
    parser = argparse.ArgumentParser(description="更新原神剧情资料（增量抓取，保留来源，不调用模型 API）")
    parser.add_argument("--config", type=Path, default=ROOT / "knowledge/sources.json")
    parser.add_argument("--limit", type=int, default=8, help="每次最多检查的页面数")
    parser.add_argument("--proxy", default="system", help="system / direct / http://127.0.0.1:端口")
    parser.add_argument("--site", help="仅更新指定站点 ID")
    parser.add_argument("--search", help="检索页面标题（须搭配 --site），不导入")
    parser.add_argument("--url", help="输入已配置百科的页面链接，自动识别站点与标题")
    parser.add_argument("--page", action="append", help="指定页面标题，可重复；须搭配 --site")
    parser.add_argument("--import-file", type=Path, help="导入已有视频 TXT，不重新下载或转写")
    parser.add_argument("--source-url", default="", help="本地导入资料的原始链接")
    parser.add_argument("--kind", choices=["analysis", "transcript"], default="analysis")
    args = parser.parse_args(argv)
    if (args.page or args.search) and not args.site:
        parser.error("--page/--search requires --site")
    state_path = ROOT / "knowledge/state.json"
    state = json.loads(state_path.read_text(encoding="utf-8")) if state_path.exists() else {}
    now = datetime.now(timezone.utc).isoformat()
    if args.import_file:
        text = args.import_file.read_text(encoding="utf-8-sig").strip()
        if not text:
            parser.error("Input file is empty")
        key = "local:" + str(args.import_file.resolve()) + ":" + args.kind
        changed = publish(ROOT, key, args.import_file.stem, text,
                          {"来源": args.source_url or str(args.import_file.resolve()),
                           "资料性质": "用户导入解析，推论非官方定论" if args.kind == "analysis"
                           else "用户导入转录，须核对原文和说话人",
                           "导入时间": now}, state, args.kind == "analysis")
        atomic_write(state_path, json.dumps(state, ensure_ascii=False, indent=2))
        print("IMPORTED" if changed else "UNCHANGED")
        return 0
    config = json.loads(args.config.read_text(encoding="utf-8-sig"))
    if args.url:
        for site in config["sites"]:
            prefix = site["page_url"].split("{title}")[0]
            if args.url.startswith(prefix):
                args.site = site["id"]
                args.page = [parse.unquote(args.url[len(prefix):].split("#")[0].split("?")[0]).replace("_", " ")]
                break
        else:
            parser.error("URL does not belong to a configured wiki")
    fetcher = Fetcher(args.proxy)
    if args.search:
        site = next((s for s in config["sites"] if s["id"] == args.site), None)
        if site is None:
            parser.error("Unknown site")
        url = site["api"] + "?" + parse.urlencode({
            "action": "query", "list": "search", "srsearch": args.search,
            "srlimit": 10, "format": "json"})
        fetcher.check_robots(url)
        data = json.loads(fetcher.read(url))
        print(json.dumps([p["title"] for p in data["query"]["search"]], ensure_ascii=True))
        return 0
    results = []
    count = 0
    failures = 0
    changed_count = 0
    for site in config["sites"]:
        if ((not site.get("enabled", True) and args.site != site["id"])
                or (args.site and site["id"] != args.site)):
            continue
        for title in args.page or site["pages"]:
            if count >= max(1, args.limit):
                break
            count += 1
            label = f'{site["id"]}: {title}'
            print(f"[{count}] {label}", flush=True)
            try:
                url, canonical, text, revision = fetcher.page(site, title)
                changed = publish(ROOT, url, canonical, text,
                                  {"来源": url, "站点": site["id"], "修订号": revision,
                                   "资料性质": "社区百科转录，非官方认证；评论与导航已排除",
                                   "抓取时间": now}, state)
                status = "UPDATED" if changed else "UNCHANGED"
                changed_count += int(changed)
                results.append({"page": label, "status": status})
                atomic_write(state_path, json.dumps(state, ensure_ascii=False, indent=2))
                print(status, flush=True)
            except (OSError, ValueError, KeyError, TypeError, RuntimeError) as exc:
                failures += 1
                # No insecure TLS fallback; old successfully imported content remains intact.
                results.append({"page": label, "status": "FAILED", "error": str(exc)})
                print(f"FAILED: {exc}", flush=True)
    atomic_write(ROOT / "knowledge/last_run.json",
                 json.dumps({"checked_at": now, "results": results}, ensure_ascii=False, indent=2))
    print(f"Checked={count} Updated={changed_count} Failed={failures}")
    print("资料位于 character/，已同步 dist/character。重启桌宠后自动重建检索索引。")
    if count == 0:
        print("No matching enabled site/pages.")
        return 2
    return 1 if failures else 0

if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except KeyboardInterrupt:
        print("\nStopped; completed pages are retained.")
        raise SystemExit(130)
