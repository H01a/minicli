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

### 2026-08-14 · 流式响应后端怎么处理？与非流式区别？JLine 如何打字机输出？

**问题**：流式响应在后端是怎么处理的？和非流式有什么区别？JLine 怎么做到像打字机一样逐个输出？

**解释**：
- 非流式：客户端发一次请求，服务端等模型生成完整个回答后一次性返回完整 JSON；客户端拿到全部内容再解析显示。等待期间没有任何输出。
- 流式（SSE）：请求里 stream:true，服务端开始生成后边生成边推送（HTTP 连接保持打开），每个小段是一个事件；客户端逐行读取、逐事件处理；模型生成完，服务端发 response.completed 结束事件后关闭连接。
- 我们的后端处理（DeepSeekClient）：OkHttp 拿到响应后逐行读 byteStream；"event:" 行是事件类型，"data:" 行是 JSON；空行分隔事件；response.output_text.delta 事件一到就调 onOutputDelta 把增量交给 UI。
- 本质区别：数据从"一个完整 JSON"变成"一串小事件"；用户从"干等"变成"边生成边看"。
- 打字机效果：不是 JLine 特有，而是"增量打印 + 立即刷新"——每个 delta 到来自动调用 writer.print(delta)（不换行）+ writer.flush()（立即把缓冲推到终端，不等换行）。连续的小增量连续 flush，看起来就像打字机逐字打出。
- 输出粒度取决于模型端的 delta 粒度（通常是词/片段），不是严格逐字符；严格逐字符需要在客户端拆字，我们没做也不需要。

### 2026-08-14 · SSE 的 delta / completed 事件理解对吗

**问题**：SSE 格式是不是就是 event 类型 + data 负载？event 类型是 delta 就代表还没说完，completed 代表说完了？

**解释**：
- 格式理解正确：SSE 每个事件是 event: 类型行 + data: JSON 负载行，空行分隔。
- "delta = 过程、completed = 结束"方向对，但要精确：一个回答由很多个 delta 事件组成（每个 delta 是一小段），客户端只有收到 response.completed 才确定"说完了"——不能从"delta 数量"或"最后是不是 delta"来判断结束，结束标志是独立的 completed 事件。
- 结束除了 completed（正常），还可能是 response.incomplete（截断，如超过 max_output_tokens）或 response.failed（失败）。
- completed 事件的 data 里也带完整 response 对象（含 usage token 统计），不是空负载。
- 还有其他事件类型（response.created、response.output_item.added 等），属于过程信号，客户端通常忽略。

### 2026-08-14 · 工具的形式与实现：Tool 接口和 ToolRegistry

**问题**：工具是以什么形式存在的，基础工具需要自己实现吗，怎么实现？

**解释**：
- 工具 = "函数 + 说明书"的 Java 对象：name（工具名）、description（用途说明）、inputSchema（入参 JSON Schema，告诉 LLM 要传什么参数）、invoke（真正干活的方法）。
- 对 LLM 来说，工具只是请求里 tools 数组中的一段 JSON 描述；模型不执行任何代码，只"读说明书 + 发调用单"（function_call）。
- 基础工具必须自己实现：LLM 厂商只提供"大脑"，读文件、搜代码、跑命令这些"手"要我们自己写；这正是本项目练手的目的。
- 实现方式：实现 Tool 接口（返回名字/说明/参数 schema + 写 invoke 逻辑）→ 注册进 ToolRegistry → Agent 循环根据 LLM 返回的 function_call 按名字查表、调用、拿结果回填。将来 MCP 工具也注册进同一个注册表，循环代码不用改。
- 关联里程碑：M2（工具层 + ReAct 主循环）。

### 2026-08-14 · LLM 调用工具链路 vs 基础文本回答

**问题**：LLM 调用工具的链路是什么样的，与基础文本回答有什么区别？

**解释**：
- 文本回答（M1 现状）：用户输入 → 一次请求 → LLM 直接生成文本 → 流式返回 → 结束。回答依据是模型训练数据，看不到当前项目实时状态。
- 工具调用（M2）：一次用户输入对应 N 次请求的内部循环：LLM 返回 function_call（工具名 + 参数 JSON）→ 本地代码执行工具 → 把结果（observation）回填进消息列表 → 再发一轮 → 直到 LLM 输出里没有 function_call、只剩文本 → 结束。
- LLM 并不真正"调用"你的代码：它只是生成一段有固定格式的 JSON 指令，真正的执行发生在你的进程里。这就是 Function Calling 名字的含义——模型输出结构化调用，而不是自然语言描述。
- 核心区别：请求次数（1 vs N）、输出形式（纯文本 vs 结构化调用 + 文本）、上下文维护（无状态一问一答 vs 客户端维护完整消息列表）、答案依据（训练数据 vs 实时工具结果）。
- 类比：文本回答像直接问老师；工具调用像老师派你先去查资料/做实验，拿到结果再回来汇报——老师不亲自跑腿，但根据你带回的真实数据下结论。
- 关联里程碑：M2（ReAct 主循环：Reason → Act → Observe → 循环）。

### 2026-08-14 · tools 参数的作用与示例

**问题**：tool 参数有什么作用？能否给个示例？

**解释**：
- tools 参数是每次请求发给 LLM 的"工具说明书列表"，放在请求体里。每个工具一条：type（function）、name（工具名）、description（干什么用）、parameters（入参 JSON Schema）。
- 作用：让模型知道"当前环境里有哪些能力可用、各叫什么、干什么、需要什么参数"。模型不执行任何工具，只读说明书并决定"该调谁、传什么参数"。
- 工具的真正实现不在请求里：请求里只有说明书，真正的 Tool 对象在本地 ToolRegistry 里。模型返回的调用单会按 name 查表执行。
- 示例（DeepSeek Responses API 请求体片段）：

```json
{
  "model": "deepseek-v4-flash",
  "input": "看看这个项目里有哪些 Java 文件",
  "tools": [
    {
      "type": "function",
      "name": "glob",
      "description": "按 glob 模式查找文件并返回路径列表",
      "parameters": {
        "type": "object",
        "properties": {
          "pattern": {"type": "string", "description": "glob 模式，如 src/**/*.java"}
        },
        "required": ["pattern"]
      }
    }
  ],
  "tool_choice": "auto"
}
```

- 注意：说明书越清晰，模型调用越准；传太多工具会消耗 token 且可能干扰决策，所以 description 要精炼。
- 关联里程碑：M2 切片 2（LLM Function Calling）。

### 2026-08-14 · LLM 怎么知道该调用工具

**问题**：如何让 LLM 知道该调用工具了？

**解释**：
- 没有魔法，靠两层：① 通过 tools 参数把说明书给它（它"知道有哪些工具可用"）；② 模型被训练出 Function Calling 能力，读用户请求后自己判断"这问题需要实时数据/外部动作"，于是输出结构化的 function_call（工具名 + 参数 JSON），而不是自然语言描述。
- 判断"该不该调"的是模型，不是我们的代码。代码只负责：把说明书发出去 → 解析输出（有 function_call 就执行并回填，没有就直接给文本答案）。
- tool_choice 可以控制决策倾向：auto（默认，模型自己判断）、none（禁止调用）、required（至少调一个）、指定工具名（强制调那个）。
- 模型输出的调用单示例：{"type":"function_call","call_id":"call_abc","name":"glob","arguments":"{\"pattern\":\"src/**/*.java\"}"}——arguments 是 JSON 字符串，本地要解析成对象再传给 Tool.invoke。
- 类比：tools 参数 = 给员工发"公司能力手册"；该不该找某部门 = 员工读需求后自己判断；function_call = 员工填的"服务申请单"；本地代码 = 执行部门，看到申请单去干活，把结果填回来。
- 为什么不由代码判断：用户需求是自然语言、千变万化，只有模型能理解"这句话背后需要调工具"，代码无法枚举所有情况——这就是"Function Calling 驱动，不是代码硬编码"。
- 关联里程碑：M2 切片 2/3（Function Calling + ReAct 主循环）。

### 2026-08-14 · ReAct 循环的一次完整链路（以天气为例）

**问题**：ReAct 循环是不是"请求=input+工具说明书 → 响应=选工具+调用JSON → 请求=工具返回结果 → 响应=退出循环+回答"？

**解释**：
- 大方向对，但有三处细节要精确：
  1. 请求字段是 tools（说明书列表），且每一轮都要带上——DeepSeek Responses API 是无状态的，说明书不会自动保留，模型下一轮可能还要调别的工具。
  2. 第二轮的 input 不是"只有工具结果"，而是完整历史：用户消息 + assistant 的 function_call + function_call_output（工具结果）。function_call_output 必须跟在对应的 function_call 后面，模型才能理解"你调了 X，结果是 Y"。
  3. 退出循环的判定：response.completed 且 output 里没有 function_call → 输出最终文本、退出；还有 function_call → 继续循环。另外设 max-steps 上限防死循环。
- 天气例子（假设注册表里有个 weather 工具）：
  - 第 1 轮请求：input=["看一下今天北京的天气"] + tools=[weather 说明书]
  - 响应 1：function_call {name:"weather", arguments:{"city":"北京"}} → 本地执行 → 结果"北京 晴 32°C"
  - 第 2 轮请求：input=[用户消息, function_call, function_call_output("北京 晴 32°C")] + tools（仍然带上）
  - 响应 2：纯文本"今天北京晴，最高 32 度" → 退出循环，展示给用户
- 模型本身不知道天气，最终答案来自工具返回的真实数据，模型只是把数据组织成自然语言。
- 关联里程碑：M2 切片 3（ReAct 主循环）。

### 2026-09-04 · MCP 协议与 M3 stdio 集成

**问题**：M3（MCP stdio 集成）是什么、有什么用、怎么实现？

**解释**：
- MCP（Model Context Protocol）= 一套标准协议，统一"AI 应用 ↔ 外部工具/数据源"的交互方式。MCP server 负责暴露工具清单和调用接口，宿主（minicli）动态发现并调用，不用为每个新工具改宿主代码。
- 为什么有用：内置工具要我们逐个手写并随版本发布；MCP 生态里有现成的文件、数据库、GitHub 等 server，接一次就能复用。这也是 PRD 里"内置 16 个 + MCP 外部 60+"的机制来源。
- 三层结构：
  1. 协议层：JSON-RPC 2.0 消息（请求/响应/通知，靠 id 配对）。生命周期固定为 initialize 握手 → initialized 通知 → tools/list 枚举 → tools/call 调用；状态机 NEW → INITIALIZING → READY → CLOSED，失败可重连。
  2. 传输层：stdio = 启动 server 子进程，stdin/stdout 传消息（每行一条 JSON，newline-delimited JSON），stderr 留给进程日志；Streamable HTTP 是 M4 的事。
  3. 注册层：tools/list 返回的每个工具（name/description/inputSchema）翻译成宿主自己的工具接口，注册进 ToolRegistry。
- 和 M2 的关系：MCP 工具 = 多了一层"远程翻译"。LLM 仍走 Function Calling，只看 ToolRegistry 里的说明书；真正干活时，适配器把调用转成 tools/call 请求发给 server，等响应取回结果。isError、超时、进程退出都转成 ToolResult.failure，不打断 ReAct 循环。
- 类比：MCP 像 USB 接口，server 像各种外设；tools/list 是插上后的设备枚举，tools/call 是调用设备功能，宿主里的适配器相当于设备驱动。
- 实现落点（仓库已预留包骨架）：mcp/protocol（JSON-RPC 编解码 + 状态机）、mcp/transport（Transport 抽象 + StdioTransport：ProcessBuilder 起进程、读 stdout 行、写 stdin）、mcp/registry（McpClient 会话 + 工具适配器动态注册）；Main 装配时按 server 配置启动并注册，agent/tools 层不用改。
- 验收闭环（M3 最小切片）：自写一个 echo server → minicli 完成 initialize/list/call → echo 工具注册进 ToolRegistry → ReActAgent 完成一次真实工具调用。
- 关联里程碑：M3（MCP stdio 集成）、M4（Streamable HTTP）。

### 2026-09-04 · MCP 与联网搜索的关系

**问题**：实现 MCP 前是否要先实现联网搜索？

**解释**：
- 不需要，两者是两条独立的能力线。MCP 是"宿主 ↔ 外部工具"的连接协议（怎么接）；联网搜索是一种具体工具能力（接什么/干什么），二者没有前置依赖。
- MCP 本身也不要求联网：M3 的 stdio 是本地进程管道，验收对象是自写的 echo server，全程不碰网络；M4 的 Streamable HTTP 只是把传输换成网络协议，仍不依赖搜索。
- 想要联网搜索时有两条路线，都不以对方为前置：
  1. 内置工具路线：直接加一个 web_search/fetch 内置工具（与 MCP 无关），改 Main 注册即可；
  2. MCP server 路线：先完成 M3 把协议打通，再连接现成的搜索类 MCP server，走动态注册。
- 本项目当前规划里没有独立的"联网搜索"里程碑（M7 是代码库检索 RAG，M8 的 CDP 是浏览器调试，都不是通用联网搜索）；所以按现有路线应直接推进 M3，搜索是以后按需加的可选扩展。
- 类比：USB 协议（MCP）和"接一个 U 盘还是接一个摄像头"（工具能力）是两回事；先把 USB 口做出来，再接什么设备都可以。
- 关联里程碑：M3（MCP stdio 集成）、M4（Streamable HTTP）。
