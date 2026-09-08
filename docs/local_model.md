# 本地大模型配置

桌宠现在默认通过 Ollama 调用专用本地模型 `dottore:7b`（基于 `qwen2.5:7b`），远程 API 配置仍然保留，切换时不需要修改 Java 代码。

## 当前配置

配置文件为 `conf/chat_config.properties`：

```properties
ai.provider=ollama
ollama.endpoint=http://127.0.0.1:11434/v1/chat/completions
ollama.model=dottore:7b
ollama.read_timeout_seconds=180
```

改成 `ai.provider=api` 即可恢复使用原来的远程 API。发布版使用 `dist/conf/chat_config.properties`，重新构建时会从项目配置同步过去。

## 启动与检查

`启动多托雷.bat` 会先静默检查 `127.0.0.1:11434`。若 Ollama 没有运行，它会调用：

```powershell
& "D:\soft_system\Ollama\ollama.exe" serve
```

查看已有模型：

```powershell
& "D:\soft_system\Ollama\ollama.exe" list
```

手动下载其他模型后，只要修改 `ollama.model` 即可，例如：

```powershell
& "D:\soft_system\Ollama\ollama.exe" pull deepseek-r1:7b
```

## 模型选择

本机是 i7-12700H、约 40 GB 内存、RTX 3050 Ti Laptop 4 GB。`dottore:7b` 基于已安装的 `qwen2.5:7b` 创建，并加入角色锁、示范对话、8192 上下文和较低随机性；7B 模型无法完全放入 4 GB 显存，部分计算会落到内存/CPU，因此首次回答和长上下文会更慢。

`deepseek-r1:7b` 可以作为推理型备选，但它更偏重推理，并不天然更擅长模仿多托雷。程序会自动剥离响应开头完整的 `<think>...</think>` 块，避免把思考过程显示在对话框里。

## 角色语气纠偏

专用模型定义位于 `ollama/Modelfile`。程序还会检测常见的温柔助手措辞；对悲伤、依赖、自我怀疑等情绪话题，会自动执行一次角色纠偏重写，因此这类回复可能比普通问答稍慢。修改 Modelfile 后运行：

```powershell
& "D:\soft_system\Ollama\ollama.exe" create dottore:7b -f ".\ollama\Modelfile"
```

## 人设与长期记忆

这次切换没有删除原有能力：

- `character/` 中的人设和资料继续由 RAG 动态加入提示词；
- 当前进程内的对话仍作为短期上下文；
- SQLite 继续保存和检索结构化长期记忆；
- 重新启动程序时不恢复旧聊天窗口，但长期记忆仍保留。

## 后续微调建议

先扩充并清洗人物资料，再制作“用户输入—多托雷回答”的高质量训练对。第一阶段优先使用 RAG、人物规则和少量示范对话，因为修改快、能够追溯来源。等数据集稳定后再做 LoRA/QLoRA；4 GB 显存不适合直接在本机微调 7B 模型，建议届时使用显存更大的设备或云端训练，训练完成后再量化并导入 Ollama。
