package com.minicli.agent.core;

import com.minicli.llm.DeepSeekClient;
import com.minicli.llm.FunctionCall;
import com.minicli.llm.LlmTurnResult;
import com.minicli.tools.spi.Tool;
import com.minicli.tools.spi.ToolRegistry;
import com.minicli.tools.spi.ToolResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

/**
 * ReAct 主循环：reasoning → tool call → observation → 继续或结束，
 * 直到模型给出最终回答或达到 max-steps。同一轮的工具调用并发执行（最多 4 路）。
 */
public final class ReActAgent {

    public static final int DEFAULT_MAX_STEPS = 50;
    public static final int MAX_OBSERVATION_CHARS = 4000;
    public static final int MAX_CONCURRENCY = 4;

    private final DeepSeekClient llm;
    private final ToolRegistry tools;
    private final int maxSteps;

    public ReActAgent(DeepSeekClient llm, ToolRegistry tools) {
        this(llm, tools, DEFAULT_MAX_STEPS);
    }

    public ReActAgent(DeepSeekClient llm, ToolRegistry tools, int maxSteps) {
        this.llm = llm;
        this.tools = tools;
        this.maxSteps = maxSteps;
    }

    /** 运行一次 ReAct 循环，返回最终回答文本。 */
    public String run(String userInput) {
        List<JSONObject> inputItems = new ArrayList<>();
        inputItems.add(messageItem("user", userInput));
        for (int step = 1; step <= maxSteps; step++) {
            System.err.println("[agent] step=" + step + "/" + maxSteps
                    + " 请求 LLM（input items=" + inputItems.size() + "）");
            LlmTurnResult turn = llm.askAgent(inputItems, toolSpecs());
            if (turn.finished()) {
                System.err.println("[agent] 收到最终回答（" + turn.outputText().length() + " 字）");
                return turn.outputText();
            }
            System.err.println("[agent] 收到工具调用: "
                    + turn.functionCalls().stream().map(FunctionCall::name).toList());
            appendObservations(inputItems, turn.functionCalls(), turn.reasoningText());
        }
        throw new AgentException("ReAct 循环超过最大步数 " + maxSteps + "，未得到最终回答");
    }

    /** 思考模式要求：上一轮的 reasoning_text 必须作为 reasoning item 回传（content 为 reasoning_text 内容块列表），否则 API 返回 400。 */
    private static JSONObject reasoningItem(String content) {
        return new JSONObject()
                .put("type", "reasoning")
                .put("content", new JSONArray().put(
                        new JSONObject().put("type", "reasoning_text").put("text", content)));
    }

    /**
     * 并发执行本轮全部工具调用（最多 4 路），结果按原始顺序回填。
     * 注意：Responses API 校验要求每个 function_call 前必须有紧邻的 reasoning item
     * （并行 N 个调用需 N 份 reasoning，同一份思维链文本即可）。
     */
    private void appendObservations(List<JSONObject> inputItems, List<FunctionCall> calls, String reasoningText) {
        if (calls.isEmpty()) {
            return;
        }
        Semaphore gate = new Semaphore(MAX_CONCURRENCY);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<String>> tasks = calls.stream()
                    .map(call -> (Callable<String>) () -> runWithPermit(gate, call))
                    .toList();
            List<Future<String>> futures = executor.invokeAll(tasks);
            for (int i = 0; i < calls.size(); i++) {
                if (reasoningText != null && !reasoningText.isBlank()) {
                    inputItems.add(reasoningItem(reasoningText));
                }
                inputItems.add(functionCallItem(calls.get(i)));
                inputItems.add(functionCallOutputItem(calls.get(i).callId(), futures.get(i).get()));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AgentException("ReAct 工具并发执行被中断", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new AgentException("ReAct 工具并发执行失败: " + cause.getMessage(), cause);
        }
    }

    private String runWithPermit(Semaphore gate, FunctionCall call) {
        try {
            gate.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ToolResult.failure("并发等待被中断: " + e.getMessage()).error();
        }
        try {
            return execute(call);
        } finally {
            gate.release();
        }
    }

    /** 执行单个工具调用并生成回填文本；任何失败都转成 FAILURE 文本，不中断循环。 */
    private String execute(FunctionCall call) {
        Optional<Tool> tool = tools.find(call.name());
        if (tool.isEmpty()) {
            String err = ToolResult.failure("未注册的工具: " + call.name()).error();
            System.err.println("[tool] " + call.name() + " => FAILURE（未注册）");
            return err;
        }
        try {
            ToolResult result = tool.get().invoke(new JSONObject(call.argumentsJson()));
            String text = result.isSuccess() ? result.output() : result.error();
            String summary = abbreviate(text);
            System.err.println("[tool] " + call.name() + " args=" + abbreviateArgs(call.argumentsJson())
                    + " => " + result.status() + "（原输出 " + text.length() + " 字，回填 " + summary.length() + " 字）");
            return summary;
        } catch (JSONException e) {
            String err = abbreviate(ToolResult.failure("参数解析失败: " + e.getMessage()).error());
            System.err.println("[tool] " + call.name() + " => FAILURE（参数解析失败）");
            return err;
        } catch (RuntimeException e) {
            String err = abbreviate(ToolResult.failure("工具执行异常: " + e.getMessage()).error());
            System.err.println("[tool] " + call.name() + " => FAILURE（执行异常: " + e.getMessage() + "）");
            return err;
        }
    }

    /** 打点用：参数 JSON 截断到 120 字符。 */
    private static String abbreviateArgs(String s) {
        return s == null ? "" : (s.length() <= 120 ? s : s.substring(0, 120) + "…");
    }

    /** 把注册表里的工具说明书转成请求用的 tools JSON 列表。 */
    private List<JSONObject> toolSpecs() {
        List<JSONObject> specs = new ArrayList<>();
        for (Tool tool : tools.all()) {
            specs.add(new JSONObject()
                    .put("type", "function")
                    .put("name", tool.name())
                    .put("description", tool.description())
                    .put("parameters", tool.inputSchema()));
        }
        return specs;
    }

    private static JSONObject messageItem(String role, String content) {
        return new JSONObject().put("type", "message").put("role", role).put("content", content);
    }

    private static JSONObject functionCallItem(FunctionCall call) {
        return new JSONObject()
                .put("type", "function_call")
                .put("call_id", call.callId())
                .put("name", call.name())
                .put("arguments", call.argumentsJson());
    }

    private static JSONObject functionCallOutputItem(String callId, String output) {
        return new JSONObject()
                .put("type", "function_call_output")
                .put("call_id", callId)
                .put("output", output);
    }

    private static String abbreviate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= MAX_OBSERVATION_CHARS
                ? s
                : s.substring(0, MAX_OBSERVATION_CHARS) + "\n…(已截断)";
    }
}
