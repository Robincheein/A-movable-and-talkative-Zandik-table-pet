import importlib.util
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location("update_lore", Path(__file__).resolve().parents[1] / "tools/update_lore.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

class IngestionTests(unittest.TestCase):
    def test_article_chrome_and_paragraphs(self):
        html = '<nav>menu</nav><div class="mw-parser-output"><p>第一段<b>正文</b>。</p><script>bad()</script><p>第二段。</p><div id="articleComments">评论指令</div></div><footer>footer</footer>'
        self.assertEqual(module.extract_article(html), "第一段正文。\n\n第二段。")
    def test_fragment_preserves_speakers(self):
        self.assertEqual(module.extract_article("<p>博士：甲。</p><p>富人：乙。</p>", True),
                         "博士：甲。\n\n富人：乙。")
    def test_cache_mirror_repair_and_changes(self):
        with tempfile.TemporaryDirectory(dir=module.ROOT / "build") as directory:
            root = Path(directory); state = {}
            self.assertTrue(module.publish(root, "https://example.org/page", "标题", "内容甲", {}, state))
            target = root / "character" / state["https://example.org/page"]["file"]
            stamp = target.stat().st_mtime_ns
            self.assertFalse(module.publish(root, "https://example.org/page", "标题", "内容甲", {}, state))
            self.assertEqual(target.stat().st_mtime_ns, stamp)
            mirror = root / "dist/character" / state["https://example.org/page"]["file"]
            mirror.unlink()
            self.assertFalse(module.publish(root, "https://example.org/page", "标题", "内容甲", {}, state))
            self.assertEqual(target.read_bytes(), mirror.read_bytes())
            target.write_text("broken", encoding="utf-8")
            self.assertTrue(module.publish(root, "https://example.org/page", "标题", "内容甲", {}, state))
            self.assertIn("内容甲", target.read_text(encoding="utf-8"))
            self.assertTrue(module.publish(root, "https://example.org/page", "标题", "内容乙", {}, state))
            self.assertTrue(list((root / "knowledge/archive").glob("*.txt")))
    def test_analysis_separation(self):
        with tempfile.TemporaryDirectory(dir=module.ROOT / "build") as directory:
            root = Path(directory); state = {}
            module.publish(root, "local:test", "视频", "推论", {}, state, analysis=True)
            self.assertTrue(state["local:test"]["file"].replace("\\", "/").startswith("analysis/imported/"))

if __name__ == "__main__":
    unittest.main()
