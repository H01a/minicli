---
> 需求与设计见 `docs/`：PRD（做什么）、技术设计（怎么设计）、实施计划（怎么推进）、进度（做到哪里）。

# minicli

类似 Claude Code 的终端 Agent CLI：在终端里用自然语言驱动代码开发与调试。
基于 ReAct 主循环、Plan-and-Execute + Multi-Agent、MCP 工具集成、三层记忆系统、
RAG 代码库检索，技术栈为 Java 21 / JLine 3.x / SQLite / JGit / CDP / Jieba / OkHttp / DeepSeek（OpenAI 兼容）。

> 当前阶段：M1 已完成；M2 进行中（统一工具抽象、Function Calling + ReAct 主循环、并发执行已完成；审计暂缓；REPL 过程展示待做），详见 [docs/04-progress.md](docs/04-progress.md)。

## 环境要求

- JDK 21（已通过 Homebrew 安装并注册，`/usr/libexec/java_home -v 21` 可找到）
- Maven 3.9+（本机 3.9.10）
- 后续里程碑还需要：DeepSeek API Key（或任意 OpenAI 兼容端点），配置在 .env（键名 DEEPSEEK_API_KEY / DEEPSEEK_MODEL）

## 快速开始（Maven 构建 / 测试 / 运行）

```bash
# 1) 确保 Java 21 生效（新开的终端通常已自动生效）
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"

# 2) 验证环境
java -version          # 期望 openjdk 21.x
mvn -version           # 期望 Java version: 21.x

# 3) 编译 + 单元测试
mvn test

# 4) 打包为可执行 jar
mvn package

# 5) 运行（进入 REPL：输入问题获得流式回答，exit 退出）
java -jar target/minicli-0.1.0-SNAPSHOT.jar
```

注意：`mvn package` 生成的 jar 由 Java 21 编译（class file 65），必须用 JDK 21 运行；
如果 `java -jar` 报 `UnsupportedClassVersionError`，说明 PATH 里的 java 还是旧版本，
先执行上面的 export 再运行。

## Git 常用指令

```bash
git status                    # 查看工作区与暂存区状态
git diff                      # 查看未暂存改动
git add <path>                # 只暂存当前任务相关文件，避免 git add -A 混入无关文件
git commit -m "chore: scaffold maven project for M0"   # 提交信息使用英文
git log --oneline             # 查看提交历史
git branch                    # 查看分支
git push                      # 推送远程（本项目未经用户授权不推送）
```

仓库约定（见 AGENTS.md）：不提交 `.env*`、密钥、数据库、日志、`target/`、`.idea/` 等；
`.gitignore` 已按此配置。

## 目录结构

```
src/main/java/com/minicli/   # 按 docs/02-design.md §3 分层（包骨架随里程碑落地）
├── Main.java               # 入口：装配 + 启动
├── config/                 # 配置加载统一入口（M0.2）
├── db/                     # SQLite 连接与迁移（M0.2）
├── ui/                     # JLine REPL（M0.2）
├── domain/                 # 纯 Java 领域模型（M1）
├── llm/                    # LLM 客户端（M1）
├── agent/{core,planner,executor,reviewer}/  # ReAct + Multi-Agent（M2/M6）
├── tools/{spi,builtin,audit}/               # 统一工具层（M2）
├── mcp/{protocol,transport,registry}/       # MCP 客户端（M3/M4）
├── memory/{model,store,compress}/           # 记忆系统（M5）
├── retrieval/{precise,rag}/                 # 检索（M7）
├── git/                     # JGit（M8）
└── browser/                 # CDP（M8）
src/test/java/com/minicli/MainTest.java  # 单元测试（含 Java 21+ 断言）
docs/                                   # PRD / 设计 / 实施 / 进度 / 工作流 / settling
```
