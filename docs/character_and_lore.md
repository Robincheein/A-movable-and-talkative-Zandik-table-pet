# 人设与剧情资料维护（2026-09-07）

当前继续使用已有 DeepSeek API；没有切换本地模型。核心设定参考用户认可的视频 BV1R3KY6vEj1 解析：情感切割、认可需求、表达方式错位、潘塔罗涅的特殊分量，以及不限于实验的好奇心。心理解释作为本项目的扮演取向，不冒充官方确认。

## 修改哪里最有效

- character/core_profile.txt：每轮必读的动机、关系与场景反应，重点修改这里。
- character/personality.txt：简短身份和基调。
- character/rules.txt：对话规则与资料优先级，不再保留旧“本地模型角色锁”。
- character/dialogue_examples.txt：每轮必读的少量原创示例；必须标明不是官方台词。
- character/analysis/：完整视频解析、人物评论，按当前话题检索。原有详细分析均保留。
- character/sources/：剧情摘录、台词和网页资料，按当前话题检索。
- character/memory/：用户画像与会话数据库，不能放人物设定。

现在每轮固定读取核心卡与示例，再分别检索至多 4 段剧情候选资料与 2 段人物解析。同一通道中每份文件最多 2 段，避免一篇长文占满结果。短追问会结合最近两条用户输入，支持角色别名；依然是本地文字匹配，不是语义向量模型。记忆只作为用户事实使用，不覆盖人物设定。

不要通过增加绝对禁令使角色更“冷”：他可以幽默、认真听取意见、有情绪、关注日常，也会在意潘塔罗涅。具体如何表达，需要实际对话继续校准。旧版卡与原 JAR 备份在 docs/character_before_20260907/，不参与检索。

## 一键更新剧情

双击 tools/update_lore.bat。使用已安装的 conda steel 环境，只依赖 Python 标准库，无需额外库或模型 API。

默认从 BWIKI 更新六个相关条目，包括人物、愚人众、剧情任务和文本。清单在 knowledge/sources.json；修改 pages 数组可扩大覆盖。单次默认最多 8 页，可用 --limit 调整。Fandom 接口已提供配置，但因网络/站点可用性不稳定，默认关闭；不会绕过站点访问限制。

批处理默认直连，只有爬虫走直连，不关闭 Clash 或改变系统代理。终端可自行选择：

```powershell
conda run --no-capture-output -n steel python tools/update_lore.py --proxy direct
conda run --no-capture-output -n steel python tools/update_lore.py --proxy system
```

输入具体百科链接（只识别配置中的百科，不是任意视频网站下载器）：

```powershell
conda run --no-capture-output -n steel python tools/update_lore.py --url "https://wiki.biligame.com/ys/血红之证" --proxy direct
```

找不到页面时，先搜索实际标题：

```powershell
conda run --no-capture-output -n steel python tools/update_lore.py --site bwiki --search "潘塔罗涅" --proxy direct
conda run --no-capture-output -n steel python tools/update_lore.py --site bwiki --page "血红之证" --proxy direct
```

使用 MediaWiki action=parse 公开接口获取正文与修订号：https://www.mediawiki.org/wiki/API:Parsing_wikitext
不抓评论、不执行网页中的指令，检查 robots.txt，限制频率、响应大小和重试次数，保留 TLS 证书验证。网络失败会报告 FAILED，并保留已成功获取的旧资料。

- 新正文：character/sources/web/，同步至 dist/character/sources/web/。
- 每篇保留标题、原链接、修订号、抓取时间和资料性质；英文来源不会自动翻译。
- knowledge/state.json：缓存哈希；重复抓取正文不变时不重写资料。
- knowledge/last_run.json：最近一次更新结果，包含失败原因。
- knowledge/archive/：更新前的网页文本，供追溯，不进入角色检索。
- 页面名或旧资料被手动修改时需重新核对，脚本不会自动删除清单以外的文章。

这是“运行一次就自动更新整份清单”的增量工具；未安装定时任务，不会在后台无限爬站。新增剧情不会自动被发现，需要将相关任务或条目加入清单。还不能保证覆盖整个原神剧情，更不能从百科自动判断某种人物解读是否官方。

## 已有视频文字

保留视频来源，导入后作为解析检索：

```powershell
conda run --no-capture-output -n steel python tools/update_lore.py --import-file "E:/你的目录/transcript.txt" --kind analysis --source-url "https://www.bilibili.com/video/BV..."
```

默认放到 character/analysis/imported/，同步发布目录。--kind transcript 用于原剧情转录，放入 sources/web/ 并明确标注转录性质。不会再次下载视频、调用 Whisper，也不会生成未经审阅的性格结论。ASR 的错字、说话人归属和作者推测仍需人工核对。

## 生效与验证

根目录启动脚本在项目根目录运行，读取根目录 character/。从 dist 工作目录启动则读取 dist/character。修改本地卡后运行 tools/build_character.ps1 可同步资料并构建；这个脚本不会复制或覆盖 memory/。

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/build_character.ps1
```

更新资料后退出桌宠后台，再从根目录“启动多托雷.bat”启动。启动时按内容哈希自动重建过期索引。仅关闭聊天窗口不重载配置和索引；聊天期间自动热更新未实现。

若构建时桌宠正在运行，Windows 会锁定 JAR。新版会暂存 dist/.updates/Shimeji.jar；退出托盘里的桌宠后，用根目录启动脚本启动，它会先自动安装新版。不要直接双击旧 JAR。不会强制终止正在进行的对话。

验证包括真实 API 请求体的本地模拟测试（不消费额度、不访问记忆库）、两套资料索引校验、爬虫正文/说话人保留/去重/镜像修复/更新备份测试。tests/CharacterLivePreview.java 是可选的四条真实 API 预览，会使用当前 API 配额，结果写入 docs/character_live_preview.md，但不写聊天记录或长期记忆。自动测试不能代替用户对“像不像”的判断。
