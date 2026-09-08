# 多托雷长期记忆数据库

数据库文件由桌宠首次启动时自动创建：

```text
character/memory/dottore_memory.db
```

源码目录运行时位于项目的 `character/memory/`；发布版运行时位于
`dist/character/memory/`。

## 使用 sqlite3 连接

先关闭桌宠，或只执行只读查询：

```powershell
cd E:\desktop\CODE\Zandik
& 'D:\soft_system\SQLite\sqlite3.exe' -header -column -box '.\dist\character\memory\dottore_memory.db'
```

进入交互界面后可执行：

```sql
.tables
.schema memory_items
SELECT * FROM memory_items_zh;
SELECT table_name, column_name, chinese_name, description
FROM schema_dictionary
ORDER BY table_name, column_name;
```

退出：

```sql
.quit
```

Java 使用以下 JDBC URL 连接同一个文件：

```text
jdbc:sqlite:E:\desktop\CODE\Zandik\dist\character\memory\dottore_memory.db
```

SQLite 没有 MySQL 的持久化字段 `COMMENT` 语法，因此正式表使用英文
`snake_case` 字段名，并通过两种方式保留中文说明：

1. `schema_dictionary` 数据字典表保存中文字段名和含义。
2. `memory_items_zh` 只读视图提供中文列名，方便人工查询。

## 核心表

- `memory_sessions`：会话摘要和内容指纹，避免重复保存。
- `conversation_messages`：会话消息明细。
- `memory_items`：当前有效的结构化长期记忆。
- `memory_revisions`：记忆更新前的旧内容，便于追溯。
- `schema_dictionary`：字段中文名称及说明。

历史会话迁移会先严格按 UTF-8 解码，失败后自动回退到 GB18030；新会话统一以 UTF-8 写入。

程序启用了外键、WAL、普通同步和 5 秒 busy timeout。不要在桌宠运行时使用
`DROP TABLE`、`DELETE` 或结构修改命令；查看数据可以正常进行。
