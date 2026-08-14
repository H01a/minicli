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

### 2026-08-14 · 概念问答追加：Responses API 参数速查

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| learning/concepts.md | 追加 | Responses API 参数速查 | 已完成（第 4 节预先授权） |

### 2026-08-13 · 概念问答追加（第 4 节常驻授权）

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| learning/concepts.md | 追加 | SQLite/JLine/Ollama 概念确认 + Embedding 解释 | 已完成（第 4 节预先授权） |

## 4. 常驻主动更新规则

- 适用文件：`learning/concepts.md`（概念问答学习记录）。
- 触发条件：用户提出概念性问题且 @root 给出解释；或开发过程中讲解关键概念（如 SQLite/JLine/ReAct/MCP）时。
- 更新方式：按"已记录问答"格式追加一条（日期 + 问题 + 大白话解释 + 关联里程碑），只追加不重写。
- 授权：本条规则经用户在对话框中确认后，视为对 learning/concepts.md 增量追加的预先授权；追加时仍需同步登记本节轮次记录（rules 第 2 条），但无需每轮重复确认（rules 第 1 条的确认要求由本条预先授权覆盖）。
- 边界：本规则仅覆盖 learning/concepts.md 的追加更新；其他任何文件修改仍须按 rules 第 1 条逐轮出 plan 确认。
