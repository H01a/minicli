# minicli 实施计划（怎么推进）

- 状态：草稿 v0.1
- 更新时间：2026-08-17

## 1. 实施原则

每个任务按最小纵向切片完成（AGENTS.md 第 4 条）：文档定位 → 数据模型/迁移 → 后端服务 → API → UI → 测试 → 文档与进度。
一次优先交付一个可验证闭环；每完成一个里程碑必须更新 `docs/04-progress.md`。

## 2. 里程碑

### M0 项目脚手架

- **M0.1 环境与工程骨架（已完成 2026-08-13）**
  - JDK 21 安装并注册（Homebrew openjdk@21 21.0.12，`/usr/libexec/java_home -v 21` 可找到）。
  - Maven 单模块工程：Java 21（`maven.compiler.release=21`）、JUnit 5、jar 带 Main-Class。
  - helloworld（`Main.greeting()`）+ 单测（含 Java 21+ 断言）；`.gitignore`、`.env.example`。
  - 验证指令与 git 指令写入 `README.md`。
  - 证据：`mvn test` 2/2 通过；`mvn package` 成功；`java -jar target/minicli-0.1.0-SNAPSHOT.jar` 输出 `Hello from minicli (Java 21.0.12)`。
- **M0.2 基础设施（进行中）**
  - 配置加载（`config/`）与统一 Config，业务代码不散读环境变量。【已完成 2026-08-14】
  - SQLite 连接 + 迁移执行器（schema_migrations 起步）。【待办】
  - JLine 最小 REPL：输入命令 → 回显 → 退出。【已完成 2026-08-14，直接对接 LLM 问答而非回显】
  - 验收：`mvn test` 通过；`mvn package` 后可运行 REPL。

### M1 LLM 接入（DeepSeek / OpenAI 兼容）（已完成 2026-08-14）

- OkHttp 客户端：DeepSeek Responses API（OpenAI 兼容）、流式输出、reasoning 透传。
- 垂直闭环：REPL 输入 → LLM 回答 → 流式渲染。
- 进度：无状态流式一问一答已交付（SSE 增量打印，response.completed 结束）；reasoning 已通过回调透传，终端展示归 M2（FR-01 验收）；多轮上下文归 M5 记忆。
- 验收：配置 .env（DEEPSEEK_API_KEY / DEEPSEEK_MODEL）后可对话；无网络/密钥缺失/服务不可用有清晰报错。
  【验收通过 2026-08-14】

### M2 工具层 + ReAct 主循环（进行中 2026-08-17）

- `Tool`/`ToolRegistry`；首批内置工具（已实现 read_file、list_dir、glob，目标 16 个）。【切片 1 已完成 2026-08-14】
- Function Calling 驱动的 ReAct 循环；4 路并发。【切片 2+3 已完成 2026-08-14：DeepSeekClient.askAgent + ReActAgent 串行循环；并发与 agent 模式主程序已完成 2026-08-16：虚拟线程 + Semaphore(4)，Main/Repl 接入 ReActAgent】
- 审计：按用户 2026-08-16 决定暂缓（AuditStore 接口方案保留，SQLite 就绪后实现）。
- thinking 模式：reasoning 按官方格式回传（并行调用每个 function_call 前一份），真实 API 400 已修复（2026-08-17）。
- 切片 5（已完成 2026-08-17）：REPL 过程展示（AgentListener + AgentDisplay，见 design §4.2）；16 个内置工具补齐（清单见 design §4.3；路径统一支持 `~` 展开；写类工具拒绝 .env* 与 .git；`mvn test` 73/73）。
- 验收：自然语言任务可触发多工具调用并最终回答；审计可查。【审计可查部分暂缓，SQLite 就绪后实现】

### M3 MCP stdio 集成

- JSON-RPC 协议层 + 生命周期状态机 + stdio 传输。
- tools/list 动态注册、tools/call 调用。
- 验收：连接一个 stdio MCP server（可自写最小 echo server）完成一次工具调用。

### M4 MCP Streamable HTTP 集成

- SSE 流式响应、失败重连。
- 验收：连接一个 HTTP MCP server 完成调用。

### M5 记忆系统

- memories 表/JSON 持久化、启动加载、去重、关键词 + 作用域检索。
- Map-Reduce 压缩（5 条分片、保留最近 3 轮）、事实提取回写。
- 验收：重启后偏好/事实可检索；压缩行为有单测覆盖。

### M6 Multi-Agent（Plan-and-Execute）

- Planner DAG、拓扑分批调度、Worker 池、Reviewer + 重试。
- 验收：多步任务正确分批执行；审核失败自动重试。

### M7 检索（精确 + RAG）

- 精确检索基准测试（<200ms）；chunk/embeddings 表；Embedding 服务（DeepSeek 未提供时另行确认 OpenAI 兼容端点）；Jieba 分词；Top-K 召回。
- 验收：基准测试记录在案；RAG 兜底返回相似片段。

### M8 JGit + CDP

- JGit 工具（status/diff/commit/log）；CDP 浏览器工具（打开页面、取 DOM、执行 JS）。
- 验收：终端里能完成一次"查看 diff → 提交"闭环；浏览器工具能连上本地 Chrome。

### M9 打磨与沉淀

- 配置项完整化、错误处理统一、性能优化、README 使用文档。
- 沉淀 vibecoding 工作流（docs/05 落地为可用 skill）。
- 验收：从零 clone 后按 README 可跑通全部里程碑示例。

## 3. 依赖关系

```
M0 → M1 → M2 → M3 → M4
        ↘  M2 → M5
        ↘  M2 → M6
        ↘  M2 → M7
        ↘  M2 → M8（M8 可与 M3-M7 并行）
所有 → M9
```

## 4. 完成标准（每个里程碑通用）

- 相关测试通过；迁移可从空库执行。
- 外部 Provider 的失败、超时、空数据、过期状态已处理。
- `docs/04-progress.md` 已更新：状态、证据、遗留问题、下一任务。
