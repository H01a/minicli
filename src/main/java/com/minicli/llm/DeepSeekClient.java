package com.minicli.llm;

import com.minicli.config.Config;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * DeepSeek Responses API（OpenAI 兼容，base_url https://api.deepseek.com，端点 POST /responses）。
 * 无状态问答，stream=true 流式输出（SSE 事件流，以 response.completed 结束，无 data: [DONE]）。
 * askStream 为字符串一问一答；askAgent 支持 input items 列表 + tools（Function Calling）。
 */
public final class DeepSeekClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String EVENT_OUTPUT_DELTA = "response.output_text.delta";
    private static final String EVENT_REASONING_DELTA = "response.reasoning_text.delta";
    private static final String EVENT_COMPLETED = "response.completed";
    private static final String EVENT_INCOMPLETE = "response.incomplete";
    private static final String EVENT_FAILED = "response.failed";
    private static final String TOOL_CHOICE_AUTO = "auto";

    private final Config config;
    private final OkHttpClient httpClient;

    public DeepSeekClient(Config config) {
        this(config, new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(120))
                .build());
    }

    /** 测试用：注入自定义 OkHttpClient（如指向 MockWebServer）。 */
    public DeepSeekClient(Config config, OkHttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    /** 流式问答（字符串输入）：onOutputDelta 收到最终回答增量，onReasoningDelta 收到思维链增量，完成后回调 onDone。 */
    public void askStream(String input, StreamHandler handler) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("input 不能为空");
        }
        JSONObject body = new JSONObject();
        body.put("model", config.model());
        body.put("input", input);
        body.put("stream", true);
        execute(body, new SseAccumulator(handler));
    }

    /** 非流式便捷方法：拼接流式输出后返回完整文本。 */
    public String ask(String input) {
        StringBuilder result = new StringBuilder();
        askStream(input, new StreamHandler() {
            @Override
            public void onOutputDelta(String delta) {
                result.append(delta);
            }

            @Override
            public void onDone() {
            }
        });
        return result.toString();
    }

    /** 无回调版 askAgent：ReAct 循环使用。 */
    public LlmTurnResult askAgent(List<JSONObject> inputItems, List<JSONObject> toolSpecs) {
        return askAgent(inputItems, toolSpecs, new StreamHandler() {
            @Override
            public void onOutputDelta(String delta) {
            }

            @Override
            public void onDone() {
            }
        });
    }

    /**
     * 流式版 askAgent：input 为消息/调用列表，tools 为工具说明书（Function Calling）。
     * 返回结构化结果（累积文本 + 工具调用），完成后回调 onDone。
     */
    public LlmTurnResult askAgent(List<JSONObject> inputItems, List<JSONObject> toolSpecs, StreamHandler handler) {
        JSONObject body = new JSONObject();
        body.put("model", config.model());
        body.put("input", new JSONArray(inputItems));
        body.put("stream", true);
        if (toolSpecs != null && !toolSpecs.isEmpty()) {
            body.put("tools", new JSONArray(toolSpecs));
            body.put("tool_choice", TOOL_CHOICE_AUTO);
        }
        SseAccumulator acc = new SseAccumulator(handler);
        execute(body, acc);
        return acc.result();
    }

    /** 公共请求执行：构造请求 → 校验响应 → 读 SSE。 */
    private void execute(JSONObject body, SseAccumulator acc) {
        String baseUrl = config.baseUrl().replaceAll("/+$", "");
        Request request = new Request.Builder()
                .url(baseUrl + "/responses")
                .addHeader("Authorization", "Bearer " + config.apiKey())
                .addHeader("Accept", "text/event-stream")
                .post(RequestBody.create(body.toString(), JSON))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = response.body() == null ? "" : response.body().string();
                System.err.println("[llm] 请求失败 HTTP " + response.code() + ": " + abbreviate(responseBody));
                throw new LlmException("DeepSeek API 请求失败（HTTP " + response.code() + "）: " + abbreviate(responseBody));
            }
            if (response.body() == null) {
                throw new LlmException("DeepSeek API 返回空响应");
            }
            readSse(response, acc);
        } catch (IOException e) {
            throw new LlmException("DeepSeek API 网络错误：" + e.getMessage(), e);
        }
    }

    /** 逐行解析 SSE 事件，直到 completed / incomplete / failed。 */
    private static void readSse(Response response, SseAccumulator acc) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
            String line;
            String eventType = null;
            StringBuilder data = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    if (eventType != null && !data.isEmpty()) {
                        acc.onEvent(eventType, data.toString());
                    }
                    eventType = null;
                    data.setLength(0);
                    if (acc.isDone()) {
                        break;
                    }
                } else if (line.startsWith("event:")) {
                    eventType = line.substring("event:".length()).trim();
                } else if (line.startsWith("data:")) {
                    if (!data.isEmpty()) {
                        data.append('\n');
                    }
                    data.append(line.substring("data:".length()).trim());
                }
            }
        }
        if (acc.streamError() != null) {
            throw acc.streamError();
        }
        if (!acc.isDone()) {
            throw new LlmException("DeepSeek API 流式响应提前结束，未收到 response.completed");
        }
    }

    /** SSE 累积器：收集输出文本/思维链/函数调用并驱动回调；completed/incomplete/failed 后置 done。 */
    private static final class SseAccumulator {

        private final StreamHandler handler;
        private final StringBuilder output = new StringBuilder();
        private final StringBuilder reasoning = new StringBuilder();
        private final List<FunctionCall> functionCalls = new ArrayList<>();
        private LlmException streamError;
        private boolean done;

        SseAccumulator(StreamHandler handler) {
            this.handler = handler;
        }

        LlmTurnResult result() {
            return new LlmTurnResult(output.toString(), reasoning.toString(), functionCalls);
        }

        LlmException streamError() {
            return streamError;
        }

        boolean isDone() {
            return done;
        }

        void onEvent(String eventType, String json) {
            if (done) {
                return;
            }
            try {
                JSONObject event = new JSONObject(json);
                switch (eventType) {
                    case EVENT_OUTPUT_DELTA -> {
                        String delta = event.getString("delta");
                        output.append(delta);
                        handler.onOutputDelta(delta);
                    }
                    case EVENT_REASONING_DELTA -> {
                        String delta = event.optString("delta");
                        reasoning.append(delta);
                        handler.onReasoningDelta(delta);
                    }
                    case EVENT_COMPLETED -> {
                        handler.onDone();
                        collectFunctionCalls(event);
                        done = true;
                    }
                    case EVENT_INCOMPLETE -> {
                        streamError = new LlmException("DeepSeek API 响应不完整（可能达到 max_output_tokens 或内容过滤）");
                        done = true;
                    }
                    case EVENT_FAILED -> {
                        JSONObject responseObj = event.optJSONObject("response");
                        String msg = responseObj != null && responseObj.has("error")
                                ? responseObj.getJSONObject("error").optString("message")
                                : "未知错误";
                        streamError = new LlmException("DeepSeek API 流式响应失败: " + msg);
                        done = true;
                    }
                    default -> {
                        // 忽略其他事件（response.created / output_item.added 等）
                    }
                }
            } catch (JSONException e) {
                streamError = new LlmException("DeepSeek API 流式事件解析失败: " + e.getMessage(), e);
            }
        }

        /** 从 completed 事件的 response.output 提取 function_call 项（携带完整 arguments）。 */
        private void collectFunctionCalls(JSONObject completedEvent) {
            JSONObject responseObj = completedEvent.optJSONObject("response");
            if (responseObj == null) {
                return;
            }
            JSONArray output = responseObj.optJSONArray("output");
            if (output == null) {
                return;
            }
            for (int i = 0; i < output.length(); i++) {
                JSONObject item = output.optJSONObject(i);
                if (item != null && "function_call".equals(item.optString("type"))) {
                    functionCalls.add(new FunctionCall(
                            item.optString("call_id", item.optString("id")),
                            item.optString("name"),
                            item.optString("arguments", "{}")));
                }
            }
        }
    }

    private static String abbreviate(String s) {
        String t = s == null ? "" : s.trim();
        return t.length() <= 200 ? t : t.substring(0, 200) + "…";
    }
}
