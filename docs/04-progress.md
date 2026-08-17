# minicli 进度（现在做到哪里）

- 更新时间：2026-08-16

## 当前阶段

**M1 已完成；M2 进行中（切片 1 统一工具抽象、切片 2+3 Function Calling + ReAct 主循环、并发执行与 agent 模式主程序已完成；审计暂缓；REPL 过程展示待做）；M0.2 SQLite 迁移执行器按用户 2026-08-14 决定暂时搁置。**

## 已完成

- 2026-08-13 任务 1：整体规划。
  - 产出：`docs/01-prd.md`、`docs/02-design.md`、`docs/03-implementation.md`、`docs/04-progress.md`、`docs/05-vibecoding-workflow.md`。
  - 证据：仓库已有 AGENTS.md；本机 Codex 配置（~/.codex/config.toml、skills 目录）已核实。
- 2026-08-13 任务 2（M0.1）：环境配置与 Maven 工程骨架。
  - 安装并注册 JDK 21.0.12（Homebrew openjdk@21 → `~/Library/Java/JavaVirtualMachines/openjdk-21.jdk`，`/usr/libexec/java_home` 默认解析为 21；`~/.zshrc` 无需改动）。
  - 创建 Maven 单模块工程（Java 21 + JUnit 5）：`pom.xml`、`Main.java`、`MainTest.java`、`.gitignore`、`.env.example`。
  - 证据：`mvn test` 2/2 通过；`mvn package` 成功；`java -jar target/minicli-0.1.0-SNAPSHOT.jar` 输出 `Hello from minicli (Java 21.0.12)`。
- 2026-08-14 任务 4（M0.2 config+REPL / M1 简单闭环）：
  - 统一 Config：读取 `.env`（`DEEPSEEK_API_KEY` / `DEEPSEEK_MODEL`），baseUrl 默认 https://api.deepseek.com。
  - DeepSeek Responses API 无状态一问一答（POST /responses，Bearer 认证）。
  - JLine REPL：输入问题 → LLM 回答 → exit 退出。
  - pom 新增 jline 3.26.3 / okhttp 4.12.0 / org.json 20250517 / mockwebserver（test）；shade 打包可执行 fat jar。
  - 证据：`mvn test` 9/9 通过；`java -jar` 真实 API 问答成功（输入问题返回 DeepSeek 回答）。
  - 遗留：SQLite 连接与迁移（M0.2 剩余）；流式输出与多轮上下文（M1 完善）。
- 2026-08-14 任务 5（M1 流式输出）：
  - DeepSeekClient 切换为 stream 模式：请求体 `stream:true`，SSE 解析（response.output_text.delta 增量、response.completed 结束、response.failed/incomplete 报错）。
  - 新增 StreamHandler 回调接口；Repl 边生成边打印（terminal.writer + flush）。
  - 证据：`mvn test` 10/10 通过；真实 API 长回答逐段流式输出成功；M1 验收全部通过。
  - 遗留：SQLite 连接与迁移（M0.2 剩余）；多轮上下文（M1 完善）。
- 2026-08-14 任务 6（M2 切片 1：统一工具抽象）：
  - tools/spi：Tool 接口、ToolResult、ToolRegistry（空名/重名拒绝、按名查找、注册顺序列出）。
  - tools/builtin：read_file、list_dir、glob 三个只读内置工具（缺参/IO 异常转 FAILURE，不抛给上层）。
  - 证据：`mvn test` 20/20 通过（新增 4 个测试类 10 个用例）。
  - 遗留：M2 切片 2（LLM Function Calling）与后续切片。
- 2026-08-14 任务 7（M2 切片 2+3：Function Calling + ReAct 主循环）：
  - llm：新增 FunctionCall / LlmTurnResult 模型；DeepSeekClient.askAgent 支持 input items 列表 + tools 说明书 + tool_choice:auto，从 response.completed 提取 function_call（SSE 解析重构为 SseAccumulator，askStream 兼容）。
  - agent/core：ReActAgent 串行主循环——维护完整 input items 历史、执行工具并回填、未注册工具/参数解析失败转 FAILURE、结果截断 4000 字符、max-steps 默认 50（任务 11 起可配置）。
  - 证据：`mvn test` 26/26 通过（新增 ReActAgentTest 4 个 + DeepSeekClientTest 2 个）。
  - 遗留：M2 切片 4（4 路并发 + 审计落库）、切片 5（REPL 展示 reasoning/工具链路、补齐 16 个工具）；tools 字段名（parameters）待真实 API 冒烟确认。
- 2026-08-16 任务 8（M2 并发执行 + agent 模式主程序）：
  - ReActAgent：同一轮工具调用并发执行（虚拟线程 + Semaphore(4)），结果按原始顺序回填；AgentException 支持 cause。
  - Main：装配 ToolRegistry（read_file/list_dir/glob）+ ReActAgent；Repl 改用 agent.run 输出最终回答（保留 👽 风格；reasoning/过程展示归切片 5）。
  - 证据：`mvn test` 28/28 通过（新增并行执行、并发上限 4 路两个测试）；`mvn package` 成功。
  - 遗留：审计按用户 2026-08-16 决定暂缓（AuditStore 接口方案保留）；切片 5（REPL 过程展示、补齐 16 个工具）。
- 2026-08-16 任务 9（修复：thinking 模式 reasoning 回传）：
  - 真实 API 报 HTTP 400（"The reasoning_text in the thinking mode must be passed back to the API"）。
  - 核实官方 create-response 接口：input 支持 reasoning item，其 content 为 reasoning_text 内容块列表；思考模式下思维链必须回传。
  - 修复：ReActAgent 在继续循环前回填 `{"type":"reasoning","content":[{"type":"reasoning_text","text":"..."}]}`。
  - 证据：`mvn test` 29/29 通过（新增 reasoning 回传回归测试）；真实 REPL 待用户验证。
- 2026-08-17 任务 10（Review 修复 + 调试打点）：
  - 移除 ReActAgent 打印完整 inputItems 的调试行；DeepSeekClient 请求失败打印收敛为摘要（不打印完整请求体）。
  - 统一打点（stderr，[agent]/[tool]/[llm] 前缀）：每轮 step 与 input items 数、工具调用名列表、工具执行结果（状态/长度）、最终回答长度、请求失败摘要。
  - reasoningItem 确认保持官方格式（type=reasoning + reasoning_text 内容块列表）。
  - 证据：`mvn test` 29/29 通过；打点走 stderr 不影响断言。
- 2026-08-17 任务 11（配置外部化重构）：
  - 梳理可外部化配置并分三类写入 .env：LLM（DEEPSEEK_API_KEY / DEEPSEEK_MODEL / DEEPSEEK_BASE_URL）、网络超时（DEEPSEEK_CONNECT_TIMEOUT_SECONDS / DEEPSEEK_READ_TIMEOUT_SECONDS）、Agent 主循环（MINICLI_AGENT_MAX_STEPS / MINICLI_AGENT_MAX_CONCURRENCY / MINICLI_AGENT_MAX_OBSERVATION_CHARS），每条含中文注释；.env.example 同步为模板。
  - Config 统一加载：.env > 同名环境变量 > 默认值，正整数非法值报错；DeepSeekClient 超时、ReActAgent 的 maxSteps/并发数/观察截断均改为读 Config；Main 装配透传。
  - 文档：AGENTS.md §0 与 docs/02-design.md §4.8 登记".env.example 仅个人模板、.env 为真实配置"说明；max-steps 默认值统一为 50。
  - 证据：`mvn test` 34/34 通过（新增 Config 3 个 + ReActAgent 1 个用例）；`.env` 原密钥值保留未变。

## 进行中

- 无。

## 下一任务

**M2 切片 5：REPL 过程展示（reasoning/工具调用链路 + 流式最终回答）+ 补齐 16 个内置工具；审计按用户 2026-08-16 决定暂缓（AuditStore 接口方案保留，SQLite 就绪后实现）。**
（M0.2 SQLite 迁移执行器仍按用户 2026-08-14 决定暂时搁置，未来需要时再推进。）

## 阻塞与注意事项

- 技术栈变更（2026-08-13）：LLM 由本地 Ollama 改为 DeepSeek 远程 API（OpenAI 兼容），密钥走 .env（DEEPSEEK_API_KEY / DEEPSEEK_MODEL）；M1 验收与 M7 RAG embedding 方案相应调整。
- `~/.m2/settings.xml` 实为 Word XML 文档（非 Maven 配置），2026-08-14 已备份后删除（settings.xml.bak-word），Maven 告警消失、构建正常；如需恢复可 mv 回原名。
- 运行 jar 时必须使用 JDK 21（`export JAVA_HOME=$(/usr/libexec/java_home -v 21)`），否则报 UnsupportedClassVersionError。
- 本环境抓取 developers.openai.com 官方手册返回 403，Codex 文件管理约定以本机实际配置为准。

## 历史记录

| 日期 | 任务 | 结果 | 遗留 |
| --- | --- | --- | --- |
| 2026-08-13 | 任务 1 整体规划 | 完成 5 份文档 | JDK 21 安装 |
| 2026-08-13 | 任务 2（M0.1）环境与骨架 | JDK 21 + Maven 工程 + helloworld 全部验证通过 | M0.2 基础设施 |
| 2026-08-13 | 任务 3 结构优化 | 新建 docs/settling.md；按 design §3 建立 22 个包骨架（package-info）；README 目录结构同步；`mvn test` 通过 | 待执行 M0.2 基础设施 |
| 2026-08-14 | 任务 4（M0.2 config+REPL / M1 雏形） | Config + DeepSeek Responses API 一问一答 + JLine REPL 全部验证通过（`mvn test` 9/9，真实 API 问答成功） | SQLite 迁移；流式/多轮上下文 |
| 2026-08-14 | 任务 5（M1 流式输出） | DeepSeekClient 切换 stream 模式，SSE 增量打印（`mvn test` 10/10，真实 API 流式输出成功） | SQLite 迁移；多轮上下文 |
| 2026-08-14 | settings.xml 清理 + SQLite 搁置决策 | Word XML 备份删除（settings.xml.bak-word），Maven 告警消失，`mvn package` 成功（10/10）；SQLite 迁移按用户决定暂时搁置 | 无 |
| 2026-08-14 | M2 切片 1 统一工具抽象 | Tool/ToolResult/ToolRegistry + read_file/list_dir/glob 及单测（`mvn test` 20/20） | M2 切片 2 Function Calling |
| 2026-08-14 | M2 切片 2+3 Function Calling + ReAct 主循环 | askAgent（tools 参数 + function_call 解析）+ ReActAgent 串行循环（`mvn test` 26/26） | 切片 4 并发+审计；切片 5 REPL |
| 2026-08-16 | M2 并发执行 + agent 模式主程序 | ReActAgent 并发（虚拟线程+Semaphore(4)），Main/Repl 接入 ReActAgent（`mvn test` 28/28，`mvn package` 成功） | 审计暂缓；切片 5 REPL 过程展示 |
