package com.minicli.agent.core;

import com.minicli.llm.DeepSeekClient;
import com.minicli.llm.FunctionCall;
import com.minicli.llm.LlmTurnResult;
import com.minicli.llm.StreamHandler;
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
    public static final int DEFAULT_MAX_CONCURRENCY = 4;
    public static final int DEFAULT_MAX_OBSERVATION_CHARS = 4000;

    private final DeepSeekClient llm;
    private final ToolRegistry tools;
    private final int maxSteps;
    private final int maxConcurrency;
    private final int maxObservationChars;

    public ReActAgent(DeepSeekClient llm, ToolRegistry tools) {
        this(llm, tools, DEFAULT_MAX_STEPS);
    }

    public ReActAgent(DeepSeekClient llm, ToolRegistry tools, int maxSteps) {
        this(llm, tools, maxSteps, DEFAULT_MAX_CONCURRENCY, DEFAULT_MAX_OBSERVATION_CHARS);
    }

    public ReActAgent(DeepSeekClient llm, ToolRegistry tools, int maxSteps,
                      int maxConcurrency, int maxObservationChars) {
        if (maxSteps <= 0 || maxConcurrency <= 0 || maxObservationChars <= 0) {
            throw new IllegalArgumentException("maxSteps / maxConcurrency / maxObservationChars 必须为正整数");
        }
        this.llm = llm;
        this.tools = tools;
        this.maxSteps = maxSteps;
        this.maxConcurrency = maxConcurrency;
        this.maxObservationChars = maxObservationChars;
    }

    /** 运行一次 ReAct 循环（无过程监听），返回最终回答文本。 */
    public String run(String userInput) {
        return run(userInput, new AgentListener() {
        });
    }

    /** 运行一次 ReAct 循环，过程事件实时转发给 listener，返回最终回答文本。 */
    public String run(String userInput, AgentListener listener) {
        List<JSONObject> inputItems = new ArrayList<>();
        inputItems.add(messageItem("user", userInput));
        for (int step = 1; step <= maxSteps; step++) {
            listener.onStep(step, maxSteps);
            System.err.println("[agent] step=" + step + "/" + maxSteps
                    + " 请求 LLM（input items=" + inputItems.size() + "）");
            LlmTurnResult turn = llm.askAgent(inputItems, toolSpecs(), new StreamHandler() {
                @Override
                public void onOutputDelta(String delta) {
                    listener.onOutputDelta(delta);
                }

                @Override
                public void onReasoningDelta(String delta) {
                    listener.onReasoningDelta(delta);
                }

                @Override
                public void onDone() {
                }
            });
            if (turn.finished()) {
                System.err.println("[agent] 收到最终回答（" + turn.outputText().length() + " 字）");
                listener.onDone();
                return turn.outputText();
            }
            System.err.println("[agent] 收到工具调用: "
                    + turn.functionCalls().stream().map(FunctionCall::name).toList());
            appendObservations(inputItems, turn.functionCalls(), turn.reasoningText(), listener);
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
    private void appendObservations(List<JSONObject> inputItems, List<FunctionCall> calls,
                                    String reasoningText, AgentListener listener) {
        if (calls.isEmpty()) {
            return;
        }
        for (FunctionCall call : calls) {
            listener.onToolCallStarted(call);
        }
        Semaphore gate = new Semaphore(maxConcurrency);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<ToolOutcome>> tasks = calls.stream()
                    .map(call -> (Callable<ToolOutcome>) () -> runWithPermit(gate, call))
                    .toList();
            List<Future<ToolOutcome>> futures = executor.invokeAll(tasks);
            for (int i = 0; i < calls.size(); i++) {
                if (reasoningText != null && !reasoningText.isBlank()) {
                    inputItems.add(reasoningItem(reasoningText));
                }
                FunctionCall call = calls.get(i);
                ToolOutcome outcome = futures.get(i).get();
                listener.onToolResult(call, outcome.result(), outcome.durationMillis());
                inputItems.add(functionCallItem(call));
                inputItems.add(functionCallOutputItem(call.callId(), resultText(outcome.result())));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AgentException("ReAct 工具并发执行被中断", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new AgentException("ReAct 工具并发执行失败: " + cause.getMessage(), cause);
        }
    }

    private ToolOutcome runWithPermit(Semaphore gate, FunctionCall call) {
        try {
            gate.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ToolOutcome(ToolResult.failure("并发等待被中断: " + e.getMessage()), 0);
        }
        long start = System.nanoTime();
        try {
            return new ToolOutcome(execute(call), (System.nanoTime() - start) / 1_000_000);
        } finally {
            gate.release();
        }
    }

    /** 执行单个工具调用；任何失败都转成 FAILURE，不中断循环。返回完整结果（回填时再截断）。 */
    private ToolResult execute(FunctionCall call) {
        Optional<Tool> tool = tools.find(call.name());
        if (tool.isEmpty()) {
            ToolResult failure = ToolResult.failure("未注册的工具: " + call.name());
            System.err.println("[tool] " + call.name() + " => FAILURE（未注册）");
            return failure;
        }
        try {
            ToolResult result = tool.get().invoke(new JSONObject(call.argumentsJson()));
            String text = result.isSuccess() ? result.output() : result.error();
            System.err.println("[tool] " + call.name() + " args=" + abbreviateArgs(call.argumentsJson())
                    + " => " + result.status() + "（原输出 " + text.length() + " 字，回填 "
                    + abbreviate(text).length() + " 字）");
            return result;
        } catch (JSONException e) {
            System.err.println("[tool] " + call.name() + " => FAILURE（参数解析失败）");
            return ToolResult.failure("参数解析失败: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("[tool] " + call.name() + " => FAILURE（执行异常: " + e.getMessage() + "）");
            return ToolResult.failure("工具执行异常: " + e.getMessage());
        }
    }

    /** 回填给 LLM 的观察文本：截断到配置长度。 */
    private String resultText(ToolResult result) {
        return abbreviate(result.isSuccess() ? result.output() : result.error());
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

    private String abbreviate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= maxObservationChars
                ? s
                : s.substring(0, maxObservationChars) + "\n…(已截断)";
    }

    /** 工具执行结果 + 耗时（毫秒）。 */
    private record ToolOutcome(ToolResult result, long durationMillis) {
    }
}
