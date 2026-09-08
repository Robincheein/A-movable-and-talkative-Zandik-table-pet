# 动物形态版 2.0.0-animal.1

## 本次内容

- 全部 `img/shime1.png` 到 `shime46.png` 已替换为新动物形态，`dist/img` 同步；托盘图标也换新并自动缩放。
- 10 张独立姿势覆盖 46 个旧动作槽位，其中部分动作共用姿势，并不是 46 张独立原画。
- 四张走路图另存为 `fox_walk_01.png` 到 `fox_walk_04.png`，行走、跑动使用四帧循环。
- 51 张运行资源均为 128×128 RGBA 透明 PNG。保留未缩小的透明原图、原始概念图、生成提示词与源文件 SHA-256。
- `Behavior.xml` 中爬墙、跳墙、吸附墙面、挂天花板、沿天花板移动的入口用 `ANIMAL_V1_DISABLED` 注释。原动作定义留在 `Actions.xml`，不在本版启用。爬墙概念图未接入。
- 下落不再在墙边吸附结束；没有候选动作时原地落下，不随机传送。拖拽松手与 8 像素区分点击/拖动的规则保留。
- Windows PowerShell 更新脚本改用真正的空字符串引用参数，修复旧脚本 `File.Replace` 的“路径格式不合法”问题；失败时显示实际原因。
- 模型/API 密钥、人设、聊天与记忆数据不在本次修改范围。

## 图片位置及再处理

- `assets/concepts/`：此前通过内置 imagegen 生成的白底概念图；提示词在同目录 `*.prompt.txt` 和 `dottore_actions/prompts.json`。
- `assets/skins/dottore_fox/`：正式透明图、未缩小 cutout、预览、映射清单 `manifest.json`。
- `img/` 和 `dist/img/`：程序实际使用的图片。

用户明确批准后，使用本地程序做白底区域连通提取和缩放；不是再次调用图片 API。背景处理保护深色轮廓内的白色毛发，并在深色背景预览中检查结果。原始参考图与概念图不覆盖。

```powershell
conda run -n steel python tools/prepare_animal_sprites.py
powershell -NoProfile -ExecutionPolicy Bypass -File tools/configure_animal_skin.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File tools/build_animal.ps1
```

重新处理图片需要 Pillow、NumPy、SciPy，普通运行不需要 Python。构建要求 JDK 8：优先使用本机安装路径，不存在时读取 `JAVA_HOME`；运行测试不加载 Java 11+ 的外置 nashorn-core。

## 验证范围

已在源码及打包 JAR 上运行 `AnimalSkinSelfTest`：

- 51 张图像均可加载，尺寸、alpha、透明区域和前景占比合格；源码/打包图片一致。
- 全部 XML 配置通过引擎加载/校验，所有姿势图片都已映射。
- 注册行为不再包含爬墙入口。
- 左右边缘落下能落地、不传送。
- 实际四帧行走推进及位移、7 像素抖动不拖动、9 像素开始拖动、松手事件分发通过。

`CharacterPromptSelfTest` 已通过：使用本地模拟 HTTP 服务测试角色提示词，不发送真实 API 请求，不读写用户记忆。未声称做过长时间真实桌面使用测试或实际模型对话测试。

## 已知限制

这是动物形态首版，原画之间仍有轻微头部/耳朵比例变化；四帧步态属于基础动画，不是人工逐帧精修。朝向仍沿用引擎镜像方式，不对称耳朵也会一起镜像。部分原引擎动作使用共用姿势，后续可逐个精修。

## 旧版及隐私

- 换图前提交：`e733560`；标签：`pre-animal-20260908`。
- 本机备份：`backups/pre-animal-20260908-082358/`，含原配置、图片、程序及私人数据；SQLite 一致性快照也在其中。该备份不上传 GitHub。
- 要查看旧代码，建议另建工作目录：`git worktree add ../Zandik-pre-animal pre-animal-20260908`，避免覆盖当前私人配置和数据库。
- `.gitignore` 排除所有运行密钥配置、个人设置、会话/数据库和备份。新机器要从 example 配置创建自己的私有配置。不要把备份或数据库手动添加进 Git。
