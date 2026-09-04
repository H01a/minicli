# settling —— 每轮对话修改文件清单

> 依据 `docs/rules/01-compulsory.md` 第 2 条：所有基于本项目的 agent 每轮强制扫描本清单；
> 本文件不存在时，涉及文件修改的轮次必须先新建它。本文件记录"每轮对话需要修改的文件清单"
> 及每个文件的生成规则。

## 1. 使用方式

1. 收到可能涉及文件修改的指令后，先在本文件"轮次记录"登记本轮计划修改/新建的文件及生成规则。
2. 按 `docs/rules/01-compulsory.md` 第 1 条生成含详细 diff 的修改 plan，人工确认后方可执行。
3. 执行完成后回填实际结果与验证证据；未执行的文件标记"未执行"并保留记录。

## 2. 文件生成规则

| 文件 | 生成规则 |
| --- | --- |
| docs/settling.md | @root 维护；每轮修改文件清单注册表；Markdown；涉及文件修改的轮次必须最先更新它；永久保留 |
| docs/01-prd.md | @root 维护；PRD（做什么）；Markdown；产品范围/验收标准变化时更新 |
| docs/02-design.md | @root 维护；技术设计（怎么设计）；Markdown；包结构/模块/表/API/依赖变化时更新 |
| docs/03-implementation.md | @root 维护；实施计划（怎么推进）；Markdown；里程碑/顺序调整时更新 |
| docs/04-progress.md | @root 维护；进度（做到哪里）；Markdown；每个任务完成时更新状态/证据/遗留/下一任务 |
| docs/05-vibecoding-workflow.md | @root 维护；工作流沉淀与复盘；Markdown；流程/技能沉淀变化时更新 |
| src/main/java/com/minicli/** | 开发 agent 维护；遵循 docs/02-design.md §3 包结构与依赖方向；Java 21；`mvn test` 验证 |
| src/test/java/com/minicli/** | 开发 agent 维护；JUnit 5；与主代码同包；`mvn test` 验证 |
| README.md | @root 维护；使用文档；Markdown；目录结构/命令变化时同步 |
| .env.example | @root 维护；配置模板；真实值不入库 |
| learning/concepts.md | @root 维护；概念问答学习记录；Markdown；用户提出概念问题并得到解释后主动追加；只追加不重写；不参与产品开发（AGENTS.md 第 8 条） |

## 3. 轮次记录

### 2026-08-13 · 结构优化（对齐 docs/02-design.md §3）

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| docs/settling.md | 新建 | 见第 2 节 | 已完成（人工确认后执行） |
| src/main/java/com/minicli/**/package-info.java | 新建（22 个） | 依 design §3；职责见确认后的 plan | 已完成（`mvn test` 2/2 通过） |
| README.md | 更新 | 目录结构小节同步包骨架 | 已完成 |
| docs/04-progress.md | 更新 | 登记结构优化任务与证据 | 已完成 |

### 2026-08-13 · 同步分支策略（AGENTS.md 第 7 节）

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| AGENTS.md | 更新 | 第 7 节同步"main 唯一主分支 + 模版分支只读基线" | 已完成（人工确认后执行） |

### 2026-08-13 · 新增概念学习记录与主动更新规则

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| learning/concepts.md | 新建 | 记录概念 Q&A；见 settling 第 2 节与第 4 节 | 已完成（人工确认后执行） |
| docs/settling.md | 更新 | 新增 learning/concepts.md 生成规则与第 4 节常驻主动更新规则 | 已完成（人工确认后执行） |

### 2026-08-13 · 技术栈变更文档同步：Ollama → DeepSeek

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| docs/01-prd.md | 更新 | 非功能需求与 FR-04 同步远程 API 选型 | 已完成（人工确认后执行） |
| docs/02-design.md | 更新 | 技术栈/架构图/llm 包/RAG/错误处理同步 | 已完成（人工确认后执行） |
| docs/03-implementation.md | 更新 | M1 改 DeepSeek 接入；M7 embedding 待定 | 已完成（人工确认后执行） |
| docs/04-progress.md | 更新 | 注意事项登记技术栈变更 | 已完成（人工确认后执行） |
| README.md | 更新 | 环境要求与技术栈改为 DeepSeek | 已完成（人工确认后执行） |
| learning/concepts.md | 追加 | "远程 DeepSeek 后还要自己实现哪些层"概念问答 | 已完成（第 4 节预先授权） |

### 2026-08-14 · M0.2 config+REPL / M1 简单闭环（DeepSeek Responses API）

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| pom.xml | 更新 | 新增 jline/okhttp/org.json/mockwebserver；shade fat jar | 已完成（人工确认后执行） |
| src/main/java/com/minicli/config/Config.java | 新建 | 统一配置加载（DEEPSEEK_*） | 已完成（人工确认后执行） |
| src/main/java/com/minicli/config/ConfigException.java | 新建 | 配置错误异常 | 已完成（人工确认后执行） |
| src/main/java/com/minicli/llm/DeepSeekClient.java | 新建 | Responses API 无状态一问一答 | 已完成（人工确认后执行） |
| src/main/java/com/minicli/llm/LlmException.java | 新建 | LLM 调用异常 | 已完成（人工确认后执行） |
| src/main/java/com/minicli/ui/Repl.java | 新建 | JLine REPL 交互 | 已完成（人工确认后执行） |
| src/main/java/com/minicli/Main.java | 更新 | 装配并启动 | 已完成（人工确认后执行） |
| src/test/java/com/minicli/config/ConfigTest.java | 新建 | Config 单测 | 已完成（`mvn test` 9/9 通过） |
| src/test/java/com/minicli/llm/DeepSeekClientTest.java | 新建 | MockWebServer 测试 | 已完成（`mvn test` 9/9 通过） |
| docs/02-design.md | 更新 | JLine 4 → 3.x 版本修正 | 已完成（人工确认后执行） |
| docs/03-implementation.md | 更新 | M0.2/M1 状态标注 | 已完成（人工确认后执行） |
| docs/04-progress.md | 更新 | 任务 4 记录 | 已完成（人工确认后执行） |
| learning/concepts.md | 追加 | Responses API / 无状态概念问答 | 已完成（第 4 节预先授权） |

### 2026-08-14 · M1 流式输出（DeepSeekClient 切换 stream 模式）

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| src/main/java/com/minicli/llm/StreamHandler.java | 新建 | 流式回调接口（onOutputDelta/onReasoningDelta/onDone） | 已完成（人工确认后执行） |
| src/main/java/com/minicli/llm/DeepSeekClient.java | 更新 | 切换 stream 模式：SSE 解析，askStream/ask 兼容 | 已完成（人工确认后执行） |
| src/main/java/com/minicli/ui/Repl.java | 更新 | 流式增量打印 | 已完成（人工确认后执行） |
| src/test/java/com/minicli/llm/DeepSeekClientTest.java | 更新 | 流式测试（delta 顺序/failed/incomplete/stream 请求） | 已完成（`mvn test` 10/10 通过） |
| docs/03-implementation.md | 更新 | M1 进度标注流式交付 | 已完成（人工确认后执行） |
| docs/04-progress.md | 更新 | 任务 5 记录 | 已完成（人工确认后执行） |
| learning/concepts.md | 追加 | SSE 流式概念问答 | 已完成（第 4 节预先授权） |

### 2026-08-14 · 修复：流式回复结束后未自动换行

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| src/main/java/com/minicli/llm/DeepSeekClient.java | 更新 | response.completed 事件调用 handler.onDone()（此前空分支未触发换行回调） | 已完成（人工确认后执行） |
| src/test/java/com/minicli/llm/DeepSeekClientTest.java | 更新 | 断言 completed 后回调 onDone，防回归 | 已完成（`mvn test` 10/10 通过，真实运行换行正常） |

### 2026-08-14 · 概念问答追加：流式处理原理与打字机输出

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| learning/concepts.md | 追加 | 流式 vs 非流式、JLine 打字机输出原理 | 已完成（第 4 节预先授权） |

### 2026-08-14 · 概念问答追加：SSE delta/completed 事件理解确认

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| learning/concepts.md | 追加 | SSE delta=过程、completed=结束的精确理解 | 已完成（第 4 节预先授权） |

### 2026-08-14 · M1 完成状态文档同步

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| README.md | 更新 | JLine 4 → 3.x；当前阶段更新；运行示例改 REPL | 已完成（人工确认后执行） |
| docs/03-implementation.md | 更新 | M1 标注完成；reasoning 展示归 M2、多轮归 M5 | 已完成（人工确认后执行） |
| docs/04-progress.md | 更新 | 当前阶段标注 M1 完成；任务 5 补验收证据 | 已完成（人工确认后执行） |

### 2026-08-14 · 优化 AGENTS.md 为项目入职导航

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| AGENTS.md | 更新 | 新增第 0 节"接手项目导航"：项目速览/文档地图/代码结构/工程进度/接手流程 | 已完成（人工确认后执行） |

### 2026-08-14 · 注释保留 testStream 手动验证测试

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| src/test/java/com/minicli/llm/DeepSeekClientTest.java | 更新 | testStream() 整体注释保留（用户手动真实验证用），不删除不执行 | 已完成（人工确认后执行） |

### 2026-08-14 · 概念问答追加：Responses API 参数速查

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| learning/concepts.md | 追加 | Responses API 参数速查 | 已完成（第 4 节预先授权） |

### 2026-08-13 · 概念问答追加（第 4 节常驻授权）

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| learning/concepts.md | 追加 | SQLite/JLine/Ollama 概念确认 + Embedding 解释 | 已完成（第 4 节预先授权） |

### 2026-08-14 · settings.xml 清理尝试 + SQLite 搁置决策

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| docs/settling.md | 更新 | 登记本轮清单 | 已完成（人工确认后执行） |
| ~/.m2/settings.xml | 备份后删除（mv 为 settings.xml.bak-word） | 外部环境文件；构建受影响则回滚 | 已完成（人工确认后执行；`mvn package` 成功、测试 10/10、告警消失） |
| docs/04-progress.md | 更新 | 登记 settings.xml 处理结果与 SQLite 搁置决策 | 已完成（人工确认后执行） |

### 2026-08-14 · 概念问答追加：工具形式与工具调用链路

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| learning/concepts.md | 追加 | "工具的形式与实现：Tool 接口和 ToolRegistry"、"LLM 调用工具链路 vs 基础文本回答"概念问答 | 已完成（第 4 节预先授权） |

### 2026-08-14 · M2 切片 1：统一工具抽象

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| src/main/java/com/minicli/tools/spi/Tool.java | 新建 | 工具抽象接口 | 已完成（人工确认后执行；`mvn test` 20/20 通过） |
| src/main/java/com/minicli/tools/spi/ToolResult.java | 新建 | 工具执行结果模型 | 已完成（人工确认后执行；`mvn test` 20/20 通过） |
| src/main/java/com/minicli/tools/spi/ToolRegistry.java | 新建 | 统一注册表 | 已完成（人工确认后执行；`mvn test` 20/20 通过） |
| src/main/java/com/minicli/tools/builtin/ReadFileTool.java | 新建 | 首批只读内置工具 | 已完成（人工确认后执行；`mvn test` 20/20 通过） |
| src/main/java/com/minicli/tools/builtin/ListDirTool.java | 新建 | 首批只读内置工具 | 已完成（人工确认后执行；`mvn test` 20/20 通过） |
| src/main/java/com/minicli/tools/builtin/GlobTool.java | 新建 | 首批只读内置工具 | 已完成（人工确认后执行；`mvn test` 20/20 通过） |
| src/test/java/com/minicli/tools/spi/ToolRegistryTest.java | 新建 | JUnit 5；与主代码同包 | 已完成（`mvn test` 20/20 通过） |
| src/test/java/com/minicli/tools/builtin/ReadFileToolTest.java | 新建 | JUnit 5；与主代码同包 | 已完成（`mvn test` 20/20 通过） |
| src/test/java/com/minicli/tools/builtin/ListDirToolTest.java | 新建 | JUnit 5；与主代码同包 | 已完成（`mvn test` 20/20 通过） |
| src/test/java/com/minicli/tools/builtin/GlobToolTest.java | 新建 | JUnit 5；与主代码同包 | 已完成（`mvn test` 20/20 通过） |
| docs/02-design.md | 更新 | §4.3 补充接口签名与首批工具 | 已完成（人工确认后执行） |
| docs/04-progress.md | 更新 | 记录切片 1 完成与下一任务 | 已完成（人工确认后执行） |

### 2026-08-14 · 概念问答追加：tools 参数与调用时机

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| learning/concepts.md | 追加 | "tools 参数的作用与示例"、"LLM 怎么知道该调用工具"概念问答 | 已完成（第 4 节预先授权） |

### 2026-08-14 · 概念问答追加：ReAct 循环完整链路

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| learning/concepts.md | 追加 | "ReAct 循环的一次完整链路（以天气为例）"概念问答 | 已完成（第 4 节预先授权） |

### 2026-08-14 · M2 切片 2+3：Function Calling + ReAct 主循环

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| src/main/java/com/minicli/llm/FunctionCall.java | 新建 | 函数调用模型 | 已完成（人工确认后执行；`mvn test` 26/26 通过） |
| src/main/java/com/minicli/llm/LlmTurnResult.java | 新建 | 单轮响应模型 | 已完成（人工确认后执行；`mvn test` 26/26 通过） |
| src/main/java/com/minicli/llm/DeepSeekClient.java | 更新 | askAgent + SSE 解析扩展（askStream 兼容） | 已完成（人工确认后执行；`mvn test` 26/26 通过） |
| src/main/java/com/minicli/agent/core/ReActAgent.java | 新建 | ReAct 主循环（串行） | 已完成（人工确认后执行；`mvn test` 26/26 通过） |
| src/main/java/com/minicli/agent/core/AgentException.java | 新建 | 循环异常 | 已完成（人工确认后执行；`mvn test` 26/26 通过） |
| src/test/java/com/minicli/agent/core/ReActAgentTest.java | 新建 | JUnit 5 + MockWebServer | 已完成（`mvn test` 26/26 通过） |
| src/test/java/com/minicli/llm/DeepSeekClientTest.java | 更新 | 新增 askAgent 测试 | 已完成（`mvn test` 26/26 通过） |
| docs/02-design.md | 更新 | §4.2 补充具体形态 | 已完成（人工确认后执行） |
| docs/04-progress.md | 更新 | 记录完成与下一任务 | 已完成（人工确认后执行） |

### 2026-08-16 · M2 切片 4（部分）：agent 模式主程序 + 并发执行（审计暂缓）

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| src/main/java/com/minicli/Main.java | 更新 | 装配 ToolRegistry/内置工具/ReActAgent | 已完成（人工确认后执行；`mvn test` 28/28、`mvn package` 成功） |
| src/main/java/com/minicli/ui/Repl.java | 更新 | 改用 ReActAgent（agent 模式，保留 👽 风格） | 已完成（人工确认后执行；`mvn test` 28/28） |
| src/main/java/com/minicli/agent/core/ReActAgent.java | 更新 | 并发执行（虚拟线程 + Semaphore(4)，按序回填） | 已完成（人工确认后执行；`mvn test` 28/28） |
| src/main/java/com/minicli/agent/core/AgentException.java | 更新 | 增加带 cause 构造器 | 已完成（人工确认后执行；`mvn test` 28/28） |
| src/test/java/com/minicli/agent/core/ReActAgentTest.java | 更新 | 新增并发测试（并行执行/上限 4 路） | 已完成（`mvn test` 28/28） |
| docs/02-design.md | 更新 | §4.3 补充并发落地状态 | 已完成（人工确认后执行） |
| docs/04-progress.md | 更新 | 记录完成；审计暂缓 | 已完成（人工确认后执行） |

### 2026-08-16 · 修复：thinking 模式 reasoning 未回传导致 HTTP 400

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| src/main/java/com/minicli/agent/core/ReActAgent.java | 更新 | 继续循环前回填 reasoning item（content 为 reasoning_text 内容块列表） | 已完成（人工确认后执行；依据官方 create-response 接口定义，`mvn test` 29/29） |
| src/test/java/com/minicli/agent/core/ReActAgentTest.java | 更新 | 新增 thinking 模式 reasoning 回传回归测试 | 已完成（`mvn test` 29/29） |
| docs/02-design.md | 更新 | §4.2 补充 reasoning 回传要求 | 已完成（人工确认后执行） |
| docs/04-progress.md | 更新 | 记录修复与官方接口核实结论 | 已完成（人工确认后执行） |

### 2026-08-17 · Review 修复 + 调试打点

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| src/main/java/com/minicli/agent/core/ReActAgent.java | 更新 | 移除整历史打印；新增 [agent]/[tool] 打点；reasoningItem 确认为官方格式 | 已完成（人工确认后执行；`mvn test` 29/29） |
| src/main/java/com/minicli/llm/DeepSeekClient.java | 更新 | 请求失败打印收敛为 [llm] 摘要打点（不打印完整请求体） | 已完成（人工确认后执行；`mvn test` 29/29） |
| docs/04-progress.md | 更新 | 记录 review 修复与打点 | 已完成（人工确认后执行） |

### 2026-08-17 · 配置外部化重构（.env 分类整理 + 加载链路重构）

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| docs/settling.md | 更新 | 登记本轮清单 | 已完成（人工确认后执行） |
| AGENTS.md | 更新 | §0 增加配置文件说明（.env.example 仅个人模板；.env 为真实配置） | 已完成（人工确认后执行） |
| docs/02-design.md | 更新 | §4.2 修正 max-steps 默认值；新增 §4.8 配置项表 | 已完成（人工确认后执行） |
| .env.example | 更新 | 重写为带详细中文注释的配置模板（LLM/网络/Agent 三类） | 已完成（人工确认后执行） |
| .env | 更新 | 按分类重排并补注释；保留现有 DEEPSEEK_API_KEY / DEEPSEEK_MODEL 值 | 已完成（人工确认后执行；原值比对一致） |
| src/main/java/com/minicli/config/Config.java | 更新 | 新增 6 个可配置项与校验（.env > 环境变量 > 默认值） | 已完成（人工确认后执行；`mvn test` 34/34） |
| src/main/java/com/minicli/llm/DeepSeekClient.java | 更新 | OkHttp 超时改读 Config | 已完成（人工确认后执行；`mvn test` 34/34） |
| src/main/java/com/minicli/agent/core/ReActAgent.java | 更新 | maxSteps/maxConcurrency/maxObservationChars 实例化可配置 | 已完成（人工确认后执行；`mvn test` 34/34） |
| src/main/java/com/minicli/Main.java | 更新 | 装配时从 Config 传入 Agent 参数 | 已完成（人工确认后执行；`mvn test` 34/34） |
| src/test/java/com/minicli/config/ConfigTest.java | 更新 | 新增默认值/自定义值/非法值测试 | 已完成（`mvn test` 34/34） |
| src/test/java/com/minicli/agent/core/ReActAgentTest.java | 更新 | 新增观察截断配置测试 | 已完成（`mvn test` 34/34） |
| README.md | 更新 | 环境要求说明 .env 为真实配置 | 已完成（人工确认后执行） |
| docs/04-progress.md | 更新 | 记录任务 11 与测试证据；修正 max-steps 默认值 | 已完成（人工确认后执行） |

### 2026-08-17 · M2 切片 5：REPL 过程展示 + 补齐 16 个内置工具 + 文档补全

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| docs/settling.md | 更新 | 登记本轮清单 | 已完成（人工确认后执行） |
| docs/04-progress.md | 更新 | 更新时间/当前阶段/历史记录补全 | 已完成（人工确认后执行） |
| docs/02-design.md | 更新 | §4.2 过程展示设计；§4.3 16 工具清单 | 已完成（人工确认后执行） |
| docs/03-implementation.md | 更新 | M2 切片 5 状态与剩余项 | 已完成（人工确认后执行） |
| README.md | 更新 | 当前阶段同步 | 已完成（人工确认后执行） |
| src/main/java/com/minicli/agent/core/AgentListener.java | 新建 | ReAct 过程事件接口 | 已完成（人工确认后执行；`mvn test` 73/73） |
| src/main/java/com/minicli/agent/core/ReActAgent.java | 更新 | run(input, listener) + 事件转发 | 已完成（人工确认后执行；`mvn test` 73/73） |
| src/main/java/com/minicli/ui/AgentDisplay.java | 新建 | 终端过程渲染 | 已完成（人工确认后执行） |
| src/main/java/com/minicli/ui/Repl.java | 更新 | 接入 AgentDisplay | 已完成（人工确认后执行） |
| src/main/java/com/minicli/tools/builtin/PathUtil.java | 新建 | ~ 展开/路径解析/敏感路径判定 | 已完成（人工确认后执行） |
| src/main/java/com/minicli/tools/builtin/CommandRunner.java | 新建 | 命令执行辅助（超时/截断） | 已完成（人工确认后执行） |
| src/main/java/com/minicli/tools/builtin/GetCwdTool.java 等 13 个工具 | 新建 | 补齐 16 工具（清单见 design §4.3） | 已完成（人工确认后执行；`mvn test` 73/73） |
| src/main/java/com/minicli/Main.java | 更新 | 注册全部 16 个工具 | 已完成（人工确认后执行；`mvn test` 73/73） |
| src/test/java/com/minicli/** | 新建/更新 | 各工具单测 + 事件链路测试 | 已完成（`mvn test` 73/73） |

### 2026-08-17 · 输出优化：过程展示标题/耗时/结构化参数与结果

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| docs/settling.md | 更新 | 登记本轮清单 | 已完成（人工确认后执行） |
| src/main/java/com/minicli/agent/core/AgentListener.java | 更新 | onToolResult 增加 durationMillis | 已完成（人工确认后执行；`mvn test` 73/73） |
| src/main/java/com/minicli/agent/core/ReActAgent.java | 更新 | 测量工具耗时；onToolCallStarted 提前到执行前 | 已完成（人工确认后执行；`mvn test` 73/73） |
| src/main/java/com/minicli/ui/AgentDisplay.java | 更新 | 思考块/工具块标题+耗时+结构化参数与输出 | 已完成（人工确认后执行） |
| src/test/java/com/minicli/agent/core/ReActAgentTest.java | 更新 | 事件监听签名与耗时断言 | 已完成（`mvn test` 73/73） |
| docs/02-design.md | 更新 | §4.2 过程展示格式描述 | 已完成（人工确认后执行） |
| docs/04-progress.md | 更新 | 记录任务 13 | 已完成（人工确认后执行） |

### 2026-09-04 · 概念问答追加：MCP 与 M3 stdio 集成原理

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| docs/settling.md | 更新 | 登记本轮清单 | 已完成 |
| learning/concepts.md | 追加 | 概念问答：MCP 是什么、有什么用、M3 怎么实现 | 已完成（第 4 节预先授权） |

### 2026-09-04 · 概念问答追加：MCP 与联网搜索的关系

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| docs/settling.md | 更新 | 登记本轮清单 | 已完成 |
| learning/concepts.md | 追加 | 概念问答：实现 MCP 是否要先实现联网搜索 | 已完成（第 4 节预先授权） |

### 2026-09-04 · 联网搜索工具 glm_web_search（OkHttp 直连 + Config 密钥管理）

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| docs/settling.md | 更新 | 登记本轮清单 | 已完成（人工确认后执行） |
| src/main/java/com/minicli/tools/builtin/web/GLMWebSearchTool.java | 重写 | 完整实现智谱网络搜索 API 调用 | 已完成（人工确认后执行；`mvn test` 83/83） |
| src/main/java/com/minicli/tools/builtin/web/package-info.java | 新建 | web 子包说明 | 已完成（人工确认后执行） |
| src/main/java/com/minicli/config/Config.java | 更新 | 新增可选 GLM_API_KEY | 已完成（人工确认后执行；`mvn test` 83/83） |
| src/main/java/com/minicli/Main.java | 更新 | 有 key 时注册 glm_web_search | 已完成（人工确认后执行；`mvn test` 83/83） |
| pom.xml | 更新 | 移除未使用的 zai-sdk | 已完成（人工确认后执行） |
| .env.example / .env | 更新 | 追加第 5 节 GLM_API_KEY（保留原值） | 已完成（人工确认后执行；原密钥未改动） |
| src/test/java/com/minicli/config/ConfigTest.java | 更新 | 默认空 + 自定义读取 | 已完成（`mvn test` 83/83） |
| src/test/java/com/minicli/tools/builtin/web/GLMWebSearchToolTest.java | 新建 | MockWebServer 测试 | 已完成（`mvn test` 83/83） |
| docs/02-design.md | 更新 | §4.3 可选工具 + §4.8 配置行 | 已完成（人工确认后执行） |
| docs/04-progress.md | 更新 | 记录任务 16 | 已完成（人工确认后执行） |

### 2026-09-04 · 概念问答追加：MCP 协议行为与实现核心

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| docs/settling.md | 更新 | 登记本轮清单 | 已完成 |
| learning/concepts.md | 追加 | 概念问答：MCP 协议行为大白话 + 实现核心 | 已完成（第 4 节预先授权） |

### 2026-09-04 · 概念问答追加：用户对 MCP 理解的三点确认

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| docs/settling.md | 更新 | 登记本轮清单 | 已完成 |
| learning/concepts.md | 追加 | 概念问答：入口/流程/工具格式理解确认 | 已完成（第 4 节预先授权） |

### 2026-09-04 · 概念问答追加：MCP 子进程角色澄清

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| docs/settling.md | 更新 | 登记本轮清单 | 已完成 |
| learning/concepts.md | 追加 | 概念问答：子进程是 MCP server，协议 I/O 在主进程内 | 已完成（第 4 节预先授权） |

### 2026-09-04 · 概念问答追加：stdio 是什么

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| docs/settling.md | 更新 | 登记本轮清单 | 已完成 |
| learning/concepts.md | 追加 | 概念问答：标准输入/输出与 MCP stdio | 已完成（第 4 节预先授权） |

### 2026-09-04 · M3 MCP stdio 集成（everything 联调）

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| docs/settling.md | 更新 | 登记本轮清单 | 已完成（人工确认后执行） |
| src/main/java/com/minicli/mcp/protocol/McpException.java | 新建 | MCP 运行期异常 | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| src/main/java/com/minicli/mcp/protocol/JsonRpc.java | 新建 | JSON-RPC 2.0 编解码工具 | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| src/main/java/com/minicli/mcp/transport/Transport.java | 新建 | 传输层抽象（M4 复用） | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| src/main/java/com/minicli/mcp/transport/StdioTransport.java | 新建 | stdio 子进程收发 newline-delimited JSON | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| src/main/java/com/minicli/mcp/registry/McpServerConfig.java | 新建 | server 配置模型（name/command/args） | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| src/main/java/com/minicli/mcp/registry/McpToolSpec.java | 新建 | tools/list 工具模型 | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| src/main/java/com/minicli/mcp/registry/McpServerLoader.java | 新建 | JSON 清单加载与校验 | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| src/main/java/com/minicli/mcp/registry/McpClient.java | 新建 | 生命周期/请求配对/工具发现与调用 | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| src/main/java/com/minicli/mcp/registry/McpTool.java | 新建 | MCP 工具适配统一 Tool（server 前缀） | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| src/main/java/com/minicli/config/Config.java | 更新 | 新增 MINICLI_MCP_SERVERS_FILE | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| src/main/java/com/minicli/Main.java | 更新 | 启动时加载 MCP 清单并注册工具 | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| .env.example / .env | 更新 | 新增第 4 节 MCP 配置说明 | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| .gitignore | 更新 | 忽略 config/mcp-servers.json 本地清单 | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| config/mcp-servers.example.json | 新建 | everything server 配置样例 | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| config/mcp-servers.json | 新建（本地） | everything 本地联调清单（不入库） | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| src/test/java/com/minicli/mcp/testing/StubMcpProcess.java | 新建 | 测试专用 MCP stdio server | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| src/test/java/com/minicli/mcp/transport/StdioTransportTest.java | 新建 | 传输层测试 | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| src/test/java/com/minicli/mcp/registry/McpClientTest.java | 新建 | 会话生命周期/工具调用测试 | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| src/test/java/com/minicli/mcp/registry/McpToolTest.java | 新建 | 适配器/注册表测试 | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| src/test/java/com/minicli/mcp/registry/McpServerLoaderTest.java | 新建 | 配置加载测试 | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| src/test/java/com/minicli/mcp/registry/McpEverythingIntegrationTest.java | 新建 | everything 冒烟（环境变量开关） | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| src/test/java/com/minicli/config/ConfigTest.java | 更新 | MCP 配置默认值/自定义值用例 | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| docs/02-design.md | 更新 | §4.1 模型落点；§4.5 MCP 客户端实现；§4.8 配置行 | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| docs/03-implementation.md | 更新 | M3 状态 | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| docs/04-progress.md | 更新 | 记录 M3 与证据 | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |
| README.md | 更新 | MCP 配置说明 | 已完成（人工确认后执行；`mvn test` 100/100，everything 冒烟通过） |

## 4. 常驻主动更新规则

- 适用文件：`learning/concepts.md`（概念问答学习记录）。
- 触发条件：用户提出概念性问题且 @root 给出解释；或开发过程中讲解关键概念（如 SQLite/JLine/ReAct/MCP）时。
- 更新方式：按"已记录问答"格式追加一条（日期 + 问题 + 大白话解释 + 关联里程碑），只追加不重写。
- 授权：本条规则经用户在对话框中确认后，视为对 learning/concepts.md 增量追加的预先授权；追加时仍需同步登记本节轮次记录（rules 第 2 条），但无需每轮重复确认（rules 第 1 条的确认要求由本条预先授权覆盖）。
- 边界：本规则仅覆盖 learning/concepts.md 的追加更新；其他任何文件修改仍须按 rules 第 1 条逐轮出 plan 确认。
