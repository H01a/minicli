# minicli vibecoding 工作流（怎么沉淀）

- 状态：草稿 v0.1
- 更新时间：2026-08-13

## 1. 目标

本项目是 vibe coding 练手项目：不只写代码，还要把"如何用 AI 高效开发这个项目"的工作流本身沉淀下来，让每次任务越来越快、越来越规范。

## 2. 文件清单与管理职责

| 文件/目录 | 作用 | 放哪里 | 谁负责维护 |
| --- | --- | --- | --- |
| AGENTS.md | AI 编码 Agent 的行为契约（语言、文档优先级、切片流程、完成标准、git 纪律） | 仓库根目录，自动生效 | 项目 |
| docs/ | 权威知识：PRD（做什么）、技术设计（怎么设计）、实施（怎么推进）、进度（做到哪里）、本文件（工作流） | 仓库 docs/ | 项目 |
| .codex/config.toml | 项目级 Codex 设置（MCP、hooks、model、sandbox 等） | 仓库 .codex/ | 项目，敏感项脱敏 |
| .codex/skills/ | 项目专属、随仓库走的 skill | 仓库 .codex/skills/ | 项目 |
| ~/.codex/AGENTS.md | 个人全局行为契约（跨项目） | 用户目录（本机已存在，当前为空） | 个人 |
| ~/.codex/config.toml | 全局 Codex 配置（模型、MCP server、插件） | 用户目录 | 个人 |
| ~/.codex/skills/ | 全局个人 skill，跨项目复用 | 用户目录 | 个人 |
| 插件（plugins） | 打包的 skill + 工具 + 配置的安装单元 | ~/.codex/plugins（本机 openai-primary-runtime 等） | 个人安装 |
| .env / .env.example | 密钥与本地配置 | .env 不入库，.env.example 入库 | 项目 |
| .gitignore | 排除 .idea、.env*、数据库、日志、target | 仓库根 | 项目 |

## 3. skills 怎么管理

三级来源：

1. 全局个人 skill：`~/.codex/skills/<name>/SKILL.md`，跨项目复用（例如"Java 21 CLI 脚手架"）。
2. 项目级 skill：仓库 `.codex/skills/<name>/SKILL.md`，随仓库走（例如"minicli 任务切片流程"）。
3. 插件 skill：由插件打包提供（本机的 documents/pdf/presentations 等即来自插件）。

管理规则：

- skill 用 SKILL.md 描述：触发条件、执行步骤、产出/验收标准；需要脚本或模板时放同目录。
- 创建/修改 skill 走 skill-creator 流程，像代码一样有文档、有测试（示例输入输出）。
- 判断放全局还是项目：只有 minicli 相关的放项目；通用流程（如"Java 项目脚手架"）放全局。

建议下一步沉淀（本次先规划，不实现）：

- 项目 skill：`minicli-task-slice` —— 自动执行"读进度 → 查 PRD/设计 → 纵向切片 → 测试 → 更新进度"。
- 项目 skill：`minicli-progress-update` —— 任务收尾时统一更新进度文档格式。
- 全局 skill：`java-cli-scaffold` —— Maven + Java 21 + JLine + SQLite 的最小工程模板。

## 4. MCP 怎么管理

要区分两类 MCP：

1. 开发期：给 Codex（开发本项目的 AI）挂外部工具。用 `codex mcp add` 管理，配置落在 `~/.codex/config.toml`（全局）或项目 `.codex/config.toml`（项目级）。本机示例：`[mcp_servers.node_repl]`。
2. 产品期：minicli 自己作为 MCP 客户端去连接用户的 MCP server。这是 FR-05 的功能，server 列表放项目 `config/mcp-servers.yaml`（或 .env 指定路径），密钥放 .env，不入库。

规则：声明随项目走、密钥不进仓库；MCP server 的启用/停用通过配置控制，不在代码里硬编码。

## 5. 每次任务的固定循环（vibecoding 工作流）

1. 读 `docs/04-progress.md`，确认当前阶段与下一任务。
2. 在 `docs/01-prd.md` 找验收标准，在 `docs/02-design.md` 找相关模块约束。
3. 按最小纵向切片实现：文档 → 迁移 → 服务 → API → UI → 测试。
4. 更新 `docs/04-progress.md`：状态、证据、遗留、下一任务。
5. 复盘：值得复用的手法沉淀进本文件或做成 skill。

## 6. 复盘机制

- 每个里程碑结束，在 docs 追加"复盘"小节：哪些提示词有效、哪些坑踩过、下一步怎么复用。
- 踩坑记录放 `docs/retrospectives/`（首次遇到时创建），避免同一个坑问两次。
