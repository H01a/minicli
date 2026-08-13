# minicli 进度（现在做到哪里）

- 更新时间：2026-08-13

## 当前阶段

**M0.1 环境与工程骨架已完成；M0.2 基础设施待办。**

## 已完成

- 2026-08-13 任务 1：整体规划。
  - 产出：`docs/01-prd.md`、`docs/02-design.md`、`docs/03-implementation.md`、`docs/04-progress.md`、`docs/05-vibecoding-workflow.md`。
  - 证据：仓库已有 AGENTS.md；本机 Codex 配置（~/.codex/config.toml、skills 目录）已核实。
- 2026-08-13 任务 2（M0.1）：环境配置与 Maven 工程骨架。
  - 安装并注册 JDK 21.0.12（Homebrew openjdk@21 → `~/Library/Java/JavaVirtualMachines/openjdk-21.jdk`，`/usr/libexec/java_home` 默认解析为 21；`~/.zshrc` 无需改动）。
  - 创建 Maven 单模块工程（Java 21 + JUnit 5）：`pom.xml`、`Main.java`、`MainTest.java`、`.gitignore`、`.env.example`。
  - 证据：`mvn test` 2/2 通过；`mvn package` 成功；`java -jar target/minicli-0.1.0-SNAPSHOT.jar` 输出 `Hello from minicli (Java 21.0.12)`。

## 进行中

- 无。

## 下一任务

**M0.2 基础设施**：

1. 配置加载（`config/`）与统一 Config。
2. SQLite 连接与迁移执行器（schema_migrations 起步）。
3. JLine 最小 REPL（输入命令 → 回显 → 退出）。
4. 更新本进度文档。

## 阻塞与注意事项

- `~/.m2/settings.xml` 内容疑似 Word 文档（Maven 每次告警 "Expected root element 'settings' but found 'w:wordDocument'"，自动忽略，不影响构建）。建议后续将真实 settings.xml 备份后删除或替换。
- 运行 jar 时必须使用 JDK 21（`export JAVA_HOME=$(/usr/libexec/java_home -v 21)`），否则报 UnsupportedClassVersionError。
- 本环境抓取 developers.openai.com 官方手册返回 403，Codex 文件管理约定以本机实际配置为准。

## 历史记录

| 日期 | 任务 | 结果 | 遗留 |
| --- | --- | --- | --- |
| 2026-08-13 | 任务 1 整体规划 | 完成 5 份文档 | JDK 21 安装 |
| 2026-08-13 | 任务 2（M0.1）环境与骨架 | JDK 21 + Maven 工程 + helloworld 全部验证通过 | M0.2 基础设施 |
| 2026-08-13 | 任务 3 结构优化 | 新建 docs/settling.md；按 design §3 建立 22 个包骨架（package-info）；README 目录结构同步；`mvn test` 通过 | 待执行 M0.2 基础设施 |
