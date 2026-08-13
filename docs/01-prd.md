# minicli PRD（做什么）

- 状态：草稿 v0.1
- 更新时间：2026-08-13
- 负责人：@root

## 1. 项目定位

minicli 是一个类似 Claude Code 的终端 Agent CLI：在终端里用自然语言驱动代码开发与调试。
它是 vibe coding + agent CLI 的练手项目，同时承担"边做边学"的角色——每一层技术（ReAct、
Multi-Agent、MCP、记忆、RAG）都要在代码里真实落地，而不是只读文档。

## 2. 目标与学习动机

1. 用 Java 21 亲手实现一个可用的终端 Agent，理解 agent 主循环与工具调用机制。
2. 通过实现来学习 ReAct、Plan-and-Execute、Multi-Agent 协作、MCP 协议、记忆系统、RAG 等概念。
3. 沉淀一套可复用的 vibe coding 工作流（见 `docs/05-vibecoding-workflow.md`）。

## 3. 功能需求

### FR-01 ReAct 主循环 + 统一工具抽象

- 基于 ReAct（Reasoning + Acting）实现 Agent 主循环：reasoning → tool call → observation → 继续或结束。
- 统一工具抽象层：内置工具 16 个，MCP 外部工具 60+，动态注册进同一个 `ToolRegistry`。
- 工具选择由 LLM Function Calling 驱动。
- 支持最多 4 路并发工具调用；每次调用写审计日志（工具名、入参、出参摘要、耗时、结果状态）。
- 支持流式输出与 reasoning 内容回传终端。

验收标准：
- 终端输入自然语言任务后，能看到 reasoning → 选工具 → 执行 → 观察 的完整链路。
- 同一轮内最多 4 个独立工具并行执行；审计日志可查询、可导出。

### FR-02 Plan-and-Execute + Multi-Agent

- Planner 将复杂任务拆解为 DAG（任务节点 + 依赖边），按依赖分批下发。
- Worker 池并行执行任务节点。
- 每个节点由独立 Reviewer 审核，不通过自动重试（可配置最大重试次数）。

验收标准：
- 一个需要多步的任务能被拆成 DAG 并分批执行，依赖顺序正确。
- Reviewer 拒绝的结果会触发重试，重试上限后可上报失败。

### FR-03 记忆系统（4 类记忆 + 上下文压缩）

- 4 类记忆：对话（dialog）、事实（fact）、摘要（summary）、工具结果（tool result）。
- 长期记忆 JSON 持久化，启动自动加载；内容去重；按关键词与项目作用域检索。
- 跨 session 保留用户偏好与项目信息。
- 上下文压缩采用 Map-Reduce：旧消息每 5 条分片摘要后合并，保留最近 3 轮完整消息。
- 自动提取跨会话稳定事实，回写长期记忆。

验收标准：
- 关闭并重启 minicli 后，用户偏好与项目事实仍可检索到。
- 长对话超过阈值后上下文被压缩，最近 3 轮消息保持完整。

### FR-04 代码库检索（精确 + RAG 兜底）

- 内置精确检索：ripgrep + glob + read_file 组合，单次检索 <200ms。
- RAG 语义检索兜底：SQLite 向量存储 + Ollama 本地 Embedding + Jieba 中文分词。

验收标准：
- 精确检索命中并返回文件/行号；性能基准测试单次 <200ms。
- 精确检索无结果或语义场景下，RAG 返回相似代码片段。

### FR-05 MCP 工具集成

- 基于 JSON-RPC 实现完整协议生命周期：initialize 握手 → capabilities 协商 → tools/list 动态发现 → tools/call 调用。
- 支持 stdio 与 Streamable HTTP 两种传输。
- 工具动态注册进统一工具层。

验收标准：
- 可连接至少一个 stdio MCP server 与一个 Streamable HTTP server，并调用其工具。
- 连接失败、超时、server 重启等异常有明确报错与重连策略。

## 4. 非功能需求

- 本地优先：LLM 与 Embedding 走本地 Ollama，默认不把代码外发。
- 可观测：工具调用、计划节点、Review 结果均有审计/日志。
- 可配置：模型地址、并发数、重试次数、MCP server 列表等统一从配置加载，不在业务代码散读环境变量。
- 可测试：核心逻辑（ReAct 循环、DAG 调度、记忆去重、压缩、检索）都有单元测试；外部依赖可 mock。

## 5. 范围外（本期不做）

- 云端/Web 形态的产品化，不接入远程托管模型服务。
- 多语言插件市场、安装器、自动更新。
- 图形化 UI；终端交互只做 JLine 文本界面。

## 6. 术语表

- ReAct：Reasoning + Acting，先推理再行动、观察后继续推理的循环。
- Function Calling：LLM 输出结构化工具调用，而非自然语言。
- MCP：Model Context Protocol，AI 应用与外部工具的标准化协议。
- RAG：Retrieval-Augmented Generation，检索增强生成。
