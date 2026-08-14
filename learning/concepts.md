# minicli 概念学习记录（Q&A）

> 记录用户在学习 agent 开发过程中提出的概念性问题与大白话解释。
> 属于学习资料，不参与产品开发（AGENTS.md 第 8 条：其他 agent 默认忽略 learning/）。
> 更新规则见 docs/settling.md 第 4 节。

## 使用方法

- 用户提出概念性问题且得到解释后，追加一条记录：日期 + 问题 + 大白话解释 + 关联里程碑。
- 只追加不重写，保留完整学习轨迹。

## 学习建议（总纲）

- 不要先学完再动手：每个里程碑只学用到的那一点点，细节在写代码时边写边懂。
- 随时可以问"这个类是干嘛的 / 为什么要这么做 / 换个简单做法行不行"。

## 已记录问答

### 2026-08-13 · minicli 整体图景：agent CLI 由什么组成

**问题**：minicli 到底是什么？我需要先知道哪些概念？

**解释**：
- minicli 是住在终端里的 AI 助理，由四部分能力组成：
  - 大脑：大语言模型（LLM），项目用本机 Ollama 提供；
  - 手：工具（读文件、搜代码、执行命令等），后续里程碑实现；
  - 笔记本：数据库（SQLite），存聊天记录、记忆、审计；
  - 嘴和耳朵：终端交互（JLine REPL）。
- M0.2 基础设施 = 打地基：配置（怎么连大脑）+ 数据库（怎么记东西）+ 终端（怎么对话）。

### 2026-08-13 · 配置加载：.env 和 Config 是干嘛的

**问题**：配置加载是什么？.env 和 Config 是什么？

**解释**：
- 配置 = 助理的设置项：Ollama 地址、模型名、并发数等。
- .env = 存放设置的本地文件（模板 .env.example 已入库，真实 .env 不入库）。
- Config = 统一读取设置的代码对象，业务代码不自己读环境变量，都找 Config 拿。
- 类比：公司统一由行政部门采购，部门不自己乱买。

### 2026-08-13 · SQLite：为什么 agent 需要数据库

**问题**：SQLite 是什么？为什么不直接用 MySQL？

**解释**：
- SQLite = 文件型数据库，整个数据库就是项目里的一个 .db 文件，无需安装服务器，本地优先、零配置。
- 用途：存聊天记录（messages）、记忆（memories）、工具调用审计（tool_calls）、检索向量（embeddings）等。
- 与 MySQL 区别：MySQL 需要独立服务；SQLite 单文件即可。

### 2026-08-13 · 迁移执行器：数据库的版本管理

**问题**：迁移（migration）和 schema_migrations 是干嘛的？

**解释**：
- 表结构会随功能增加而变化；每次修改表结构写一份编号脚本（迁移），启动时按编号依次执行，没执行过的才执行。
- schema_migrations = 记录"已执行到第几号"的表，相当于数据库的 git 提交记录。
- 好处：新机器从空库建出完整结构；老机器只跑新增迁移，不重复建表。

### 2026-08-13 · JLine 与 REPL：终端怎么和人对话

**问题**：REPL 是什么？JLine 是干嘛的？

**解释**：
- REPL = Read-Eval-Print Loop（读入→处理→打印→循环）：你敲一行，程序处理并回复，然后继续等你。
- JLine = Java 的终端交互库：历史记录、方向键、多行输入、自动补全、流式渲染体验。
- M0.2 最小 REPL = 输入 hello 回显 hello、输入 exit 退出；M1 起把回显换成真正的 LLM 回答。

### 2026-08-13 · 为什么现在做基础设施而不是直接做 AI

**问题**：为什么不直接做 AI 功能，要先做基础设施？

**解释**：
- 依赖关系：没有 Config，接 Ollama 时模型地址只能写死；没有 SQLite，记忆/审计无处落盘；没有 REPL，没有对话界面。
- 实施计划 M0 → M1 → M2 → … 就是"先打地基再盖楼"，每层都建立在前一层上。

### 2026-08-13 · 概念确认：SQLite、JLine、Ollama 我理解对了吗

**问题**：SQLite 是跟随项目的数据库？JLine 是 REPL 交互窗口？Ollama 本地跑不了，可以直接用 DeepSeek 远程 API 吗？

**解释**：
- SQLite：方向对，精确说法是"以单个文件形式存在的数据库"（整个库就是项目里的一个 .db 文件），无需安装服务。
- JLine：需要区分两个概念——REPL 是交互形态（读入→处理→打印→循环），JLine 是实现终端交互体验的 Java 库（历史、方向键、多行、补全）。程序用 JLine 把 REPL 做好。
- Ollama：理解正确。Ollama 是本地模型运行器；本地跑不了模型就改用远程 API。DeepSeek 提供 OpenAI 兼容 API，LLM 层设计为"OpenAI 兼容客户端"，通过 .env 配置 API Key 与模型名；Ollama 从技术栈移除。
- 注意：移除 Ollama 不等于移除 Embedding 功能。Embedding 是 RAG 语义检索的必要技术（文字→向量→比相似度），只是实现载体从"Ollama 本地"改为"M7 确认的 OpenAI 兼容 Embedding 服务"。

### 2026-08-13 · Embedding 是什么？为什么 RAG 需要它

**问题**：embedding 是啥意思？

**解释**：
- Embedding = 把一段文字（词/句/代码片段）变成一串数字（向量），让计算机能比较"意思相近程度"。
- 原理：意思相近的文字，它们的向量在空间里距离近。搜索从"字面匹配"变成"距离最近"。
- 类比：把每段文字翻译成地图上的一个坐标，意思相近的挨得近；搜索变成"找离我最近的点"。
- 在 minicli 里（M7 RAG）：代码文件切小块 → 每块变向量存 SQLite → 提问也变向量 → 返回距离最近的代码片段，实现"语义检索兜底"（搜"帮我调并发"也能找到相关代码，即使没出现相同关键词）。
- 注意：DeepSeek 目前不提供 Embedding API，M7 需另选 OpenAI 兼容的 Embedding 端点。

### 2026-08-13 · 用远程 DeepSeek 后，embedding / sqlite / jieba / rag 还要自己实现吗

**问题**：决定用 API 连接远程 DeepSeek 的话，embedding、SQLite、Jieba、RAG 这些还需要自己实现吗？

**解释**：
- 是的，这些都要自己写代码实现；变的是"模型/服务"由谁提供，不是"代码"由谁写。
- SQLite：嵌入式数据库库，连接、建表、迁移代码自己写（它只是库，没有服务端）。
- Jieba：第三方中文分词库，直接引入依赖使用，集成逻辑自己写。
- Embedding：模型由远程 OpenAI 兼容 API 提供（DeepSeek 没有，M7 需另选 provider）；但"怎么调 API、怎么切 chunk、怎么存向量、怎么算相似度"的代码自己写。
- RAG：整个流程自己实现——chunk 切分 → embedding → 向量存储 → 相似度检索 → 检索结果拼进 prompt 发给 LLM。
- 这正是本项目的学习意义：不套现成 agent 框架，每一层（ReAct、MCP、记忆、RAG）都亲手实现。

### 2026-08-14 · DeepSeek Responses API 与"无状态"是什么意思

**问题**：create-response 是什么？"无状态"是什么意思？

**解释**：
- Responses API 是 OpenAI 推出的新一代对话接口格式（POST /responses），DeepSeek 兼容它，base_url 为 https://api.deepseek.com。
- 请求体：model（必填）+ input（你的问题字符串或消息列表）+ 可选 instructions（系统指令）、stream、temperature 等。
- 响应：output[] 里依次是 reasoning（思维链）和 message（最终回答，content 里的 output_text.text）。
- "无状态" = 服务端不保存你的会话；每次请求独立，多轮对话需要客户端自己把历史拼进 input 回传。所以一问一答最简单：每轮只发当前问题，不背历史。
- 为什么适合现在：先打通"输入 → 模型 → 回答"的最小闭环，多轮记忆是 M5 的事。

### 2026-08-14 · DeepSeek 对话 API（Responses API）有哪些参数

**问题**：DeepSeek 的对话 API 都有哪些参数，每个参数是什么意思？

**解释**：我们用的是 Responses API（POST /responses，OpenAI 兼容格式），主要请求参数：
- model（必填）：模型 ID，deepseek-v4-flash / deepseek-v4-pro。
- input（与 instructions 至少传一个）：纯字符串（视作一条 user 消息）或输入 item 列表（message/function_call/function_call_output/reasoning/web_search_call；消息角色 user/assistant/system/developer）。
- instructions（可选）：系统级指令，作为模型上下文的第一条 system 消息。
- stream（可选）：true 时以语义化 SSE 流式返回，事件流以 response.completed / incomplete / failed 结束（无 data: [DONE]）。
- temperature（可选，0~2，默认 1）：采样随机性，越高越随机；思考模式下不生效。
- top_p（可选，<=1，默认 1）：核采样，作为 temperature 的替代；思考模式下不生效。
- max_output_tokens（可选）：响应可生成 token 上限（含可见输出与思维链）。
- reasoning.effort（可选）：思考强度 none/minimal/low/medium/high/xhigh/max；不传则用模型默认（默认开启思考）。
- text.format（可选）：text / json_object / json_schema（结构化输出）。
- tools / tool_choice（可选）：function / web_search（服务端执行）工具及调用策略（none/auto/required/指定工具）。
- top_logprobs（可选，0~20）：每个输出位置返回 Top-N token 的对数概率。
- user（可选）：终端用户标识，用于内容安全审核与 KV 缓存隔离，勿含隐私信息。

响应侧：status（in_progress/completed/incomplete/failed）、output[]（reasoning 思维链 + message 最终回答 + function_call + web_search_call）、usage（input/output/reasoning/total tokens）、error。

现在的 DeepSeekClient 只用了 model + input；M1 流式会用到 stream，M2 工具层会用到 tools/tool_choice。

### 2026-08-14 · SSE 流式是什么

**问题**：stream 模式是怎么工作的？什么是 SSE？

**解释**：
- SSE = Server-Sent Events，服务器向客户端持续推送事件的一种 HTTP 方式。请求体加 stream:true 后，响应不是一次性 JSON，而是逐事件到达的文本流（Content-Type: text/event-stream）。
- 格式：每个事件两行（event: 类型 + data: JSON），空行分隔；客户端逐行读取。
- 关键事件：response.output_text.delta（最终回答的文本增量）、response.reasoning_text.delta（思维链增量）、response.completed（正常结束，携带 usage）、response.incomplete（截断）、response.failed（失败）。
- DeepSeek 的流没有 OpenAI 传统的 data: [DONE] 消息，以 response.completed 结束。
- 为什么用流式：回答边生成边显示，用户不用干等，体验接近真实聊天；REPL 里用回调把 delta 直接打到终端。
- 实现：DeepSeekClient.askStream(input, handler) 解析 SSE，onOutputDelta 打印增量；ask() 拼接增量返回完整文本。
