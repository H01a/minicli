# minicli 进度（现在做到哪里）

- 更新时间：2026-08-14

## 当前阶段

**M0.1 完成；M0.2 进行中（config + JLine REPL 已完成，SQLite 迁移待办）；M1 无状态流式一问一答闭环已交付（多轮上下文待完善）。**

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
  - 证据：`mvn test` 10/10 通过；真实 API 长回答逐段流式输出成功。
  - 遗留：SQLite 连接与迁移（M0.2 剩余）；多轮上下文（M1 完善）。

## 进行中

- 无。

## 下一任务

**M0.2 剩余：SQLite 连接与迁移执行器（schema_migrations 起步）。**

## 阻塞与注意事项

- 技术栈变更（2026-08-13）：LLM 由本地 Ollama 改为 DeepSeek 远程 API（OpenAI 兼容），密钥走 .env（DEEPSEEK_API_KEY / DEEPSEEK_MODEL）；M1 验收与 M7 RAG embedding 方案相应调整。
- `~/.m2/settings.xml` 内容疑似 Word 文档（Maven 每次告警 "Expected root element 'settings' but found 'w:wordDocument'"，自动忽略，不影响构建）。建议后续将真实 settings.xml 备份后删除或替换。
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
