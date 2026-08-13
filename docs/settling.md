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

## 3. 轮次记录

### 2026-08-13 · 结构优化（对齐 docs/02-design.md §3）

| 文件 | 动作 | 生成规则摘要 | 状态 |
| --- | --- | --- | --- |
| docs/settling.md | 新建 | 见第 2 节 | 已完成（人工确认后执行） |
| src/main/java/com/minicli/**/package-info.java | 新建（22 个） | 依 design §3；职责见确认后的 plan | 已完成（`mvn test` 2/2 通过） |
| README.md | 更新 | 目录结构小节同步包骨架 | 已完成 |
| docs/04-progress.md | 更新 | 登记结构优化任务与证据 | 已完成 |
