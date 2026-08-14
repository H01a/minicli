package com.minicli.llm;

import com.minicli.config.Config;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * DeepSeek Responses API（OpenAI 兼容，base_url https://api.deepseek.com，端点 POST /responses）。
 * 无状态一问一答，stream=true 流式输出（SSE 事件流，以 response.completed 结束，无 data: [DONE]）。
 */
public final class DeepSeekClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String EVENT_OUTPUT_DELTA = "response.output_text.delta";
    private static final String EVENT_REASONING_DELTA = "response.reasoning_text.delta";
    private static final String EVENT_COMPLETED = "response.completed";
    private static final String EVENT_INCOMPLETE = "response.incomplete";
    private static final String EVENT_FAILED = "response.failed";

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

    /** 流式问答：onOutputDelta 收到最终回答增量，onReasoningDelta 收到思维链增量，完成后回调 onDone。 */
    public void askStream(String input, StreamHandler handler) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("input 不能为空");
        }
        JSONObject body = new JSONObject();
        body.put("model", config.model());
        body.put("input", input);
        body.put("stream", true);

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
                throw new LlmException("DeepSeek API 请求失败（HTTP " + response.code() + "）: " + abbreviate(responseBody));
            }
            if (response.body() == null) {
                throw new LlmException("DeepSeek API 返回空响应");
            }
            readSse(response, handler);
        } catch (IOException e) {
            throw new LlmException("DeepSeek API 网络错误：" + e.getMessage(), e);
        }
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

    /** 逐行解析 SSE 事件，直到 response.completed / response.incomplete / response.failed。 */
    private static void readSse(Response response, StreamHandler handler) throws IOException {
        LlmException streamError = null;
        boolean done = false;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
            String line;
            String eventType = null;
            StringBuilder data = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    if (eventType != null && !data.isEmpty()) {
                        LlmException e = handleSseEvent(eventType, data.toString(), handler);
                        if (e != null) {
                            streamError = e;
                        }
                        if (EVENT_COMPLETED.equals(eventType) || EVENT_INCOMPLETE.equals(eventType)) {
                            done = true;
                        }
                    }
                    eventType = null;
                    data.setLength(0);
                    if (done) {
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
        if (streamError != null) {
            throw streamError;
        }
        if (!done) {
            throw new LlmException("DeepSeek API 流式响应提前结束，未收到 response.completed");
        }
    }

    /** 处理单个 SSE 事件；返回需要抛出的错误（若有）。 */
    private static LlmException handleSseEvent(String eventType, String json, StreamHandler handler) {
        try {
            JSONObject event = new JSONObject(json);
            switch (eventType) {
                case EVENT_OUTPUT_DELTA -> handler.onOutputDelta(event.getString("delta"));
                case EVENT_REASONING_DELTA -> handler.onReasoningDelta(event.optString("delta"));
                case EVENT_COMPLETED -> handler.onDone();
                case EVENT_INCOMPLETE -> {
                    return new LlmException("DeepSeek API 响应不完整（可能达到 max_output_tokens 或内容过滤）");
                }
                case EVENT_FAILED -> {
                    JSONObject responseObj = event.optJSONObject("response");
                    String msg = responseObj != null && responseObj.has("error")
                            ? responseObj.getJSONObject("error").optString("message")
                            : "未知错误";
                    return new LlmException("DeepSeek API 流式响应失败: " + msg);
                }
                default -> {
                    // 忽略其他事件（response.created / output_item.added 等）
                }
            }
            return null;
        } catch (JSONException e) {
            return new LlmException("DeepSeek API 流式事件解析失败: " + e.getMessage(), e);
        }
    }

    private static String abbreviate(String s) {
        String t = s == null ? "" : s.trim();
        return t.length() <= 200 ? t : t.substring(0, 200) + "…";
    }
}
