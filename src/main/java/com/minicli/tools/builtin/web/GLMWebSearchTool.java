package com.minicli.tools.builtin.web;

import com.minicli.tools.spi.Tool;
import com.minicli.tools.spi.ToolResult;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

/** 智谱 GLM 联网搜索工具；API Key 由 Config（.env 的 GLM_API_KEY）统一管理，经构造器注入。 */
public final class GLMWebSearchTool implements Tool {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String SEARCH_URL = "https://open.bigmodel.cn/api/paas/v4/web_search";
    private static final Set<String> ENGINES = Set.of(
            "search_std", "search_pro", "search_pro_sogou", "search_pro_quark");
    private static final Set<String> RECENCIES = Set.of("oneDay", "oneWeek", "oneMonth", "oneYear", "noLimit");

    private final String apiKey;
    private final String searchUrl;
    private final OkHttpClient httpClient;

    public GLMWebSearchTool(String apiKey) {
        this(apiKey, SEARCH_URL, defaultClient());
    }

    /** 测试用：注入自定义端点与 OkHttpClient（如指向 MockWebServer）。 */
    GLMWebSearchTool(String apiKey, String searchUrl, OkHttpClient httpClient) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.searchUrl = searchUrl;
        this.httpClient = httpClient;
    }

    private static OkHttpClient defaultClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(60))
                .build();
    }

    @Override
    public String name() {
        return "glm_web_search";
    }

    @Override
    public String description() {
        return "联网搜索（智谱 GLM）：返回适合大模型处理的结构化网页结果（标题/URL/摘要/网站名/发布时间），"
                + "支持时间范围与搜索引擎选择。适合需要实时或外部信息的任务。";
    }

    @Override
    public JSONObject inputSchema() {
        return new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("search_query", new JSONObject()
                                .put("type", "string")
                                .put("description", "搜索内容，不超过 70 个字符"))
                        .put("search_recency_filter", new JSONObject()
                                .put("type", "string")
                                .put("description", "时间范围：oneDay/oneWeek/oneMonth/oneYear/noLimit，默认 noLimit"))
                        .put("search_engine", new JSONObject()
                                .put("type", "string")
                                .put("description", "搜索引擎：search_std/search_pro/search_pro_sogou/search_pro_quark，默认 search_std"))
                        .put("count", new JSONObject()
                                .put("type", "integer")
                                .put("description", "返回条数 1-50，默认 10")))
                .put("required", new JSONArray().put("search_query"));
    }

    @Override
    public ToolResult invoke(JSONObject args) {
        String query = args.optString("search_query", "").trim();
        if (query.isBlank()) {
            return ToolResult.failure("缺少必填参数 search_query");
        }
        if (query.length() > 70) {
            return ToolResult.failure("search_query 不能超过 70 个字符");
        }
        if (apiKey.isBlank()) {
            return ToolResult.failure("未配置 GLM_API_KEY，无法执行联网搜索");
        }
        int count = args.optInt("count", 10);
        if (count < 1 || count > 50) {
            return ToolResult.failure("count 需在 1..50 之间");
        }
        String engine = args.optString("search_engine", "search_std");
        String recency = args.optString("search_recency_filter", "noLimit");
        if (!ENGINES.contains(engine)) {
            return ToolResult.failure("无效的 search_engine: " + engine);
        }
        if (!RECENCIES.contains(recency)) {
            return ToolResult.failure("无效的 search_recency_filter: " + recency);
        }

        JSONObject body = new JSONObject();
        body.put("search_query", query);
        body.put("search_engine", engine);
        body.put("search_intent", false);
        body.put("count", count);
        body.put("search_recency_filter", recency);

        Request request = new Request.Builder()
                .url(searchUrl)
                .header("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(body.toString(), JSON))
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            String text = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                return ToolResult.failure("GLM 搜索失败（HTTP " + response.code() + "）: " + abbreviate(text));
            }
            JSONObject json = new JSONObject(text);
            if (json.has("error")) {
                JSONObject err = json.getJSONObject("error");
                return ToolResult.failure("GLM 搜索失败: " + err.optString("code") + " " + err.optString("message"));
            }
            JSONArray results = json.optJSONArray("search_result");
            if (results == null || results.isEmpty()) {
                return ToolResult.success("(无搜索结果)");
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < results.length(); i++) {
                JSONObject r = results.getJSONObject(i);
                sb.append("[").append(i + 1).append("] ").append(r.optString("title")).append('\n');
                sb.append("    link: ").append(r.optString("link")).append('\n');
                String media = r.optString("media");
                String publish = r.optString("publish_date");
                if (!media.isBlank() || !publish.isBlank()) {
                    sb.append("    source: ").append(media)
                            .append(publish.isBlank() ? "" : " | publish: " + publish).append('\n');
                }
                String content = r.optString("content");
                if (!content.isBlank()) {
                    sb.append("    content: ").append(content).append('\n');
                }
            }
            return ToolResult.success(sb.toString().trim());
        } catch (JSONException e) {
            return ToolResult.failure("GLM 搜索响应解析失败: " + e.getMessage());
        } catch (IOException e) {
            return ToolResult.failure("GLM 搜索网络错误: " + e.getMessage());
        }
    }

    private static String abbreviate(String s) {
        String t = s == null ? "" : s.trim();
        return t.length() <= 300 ? t : t.substring(0, 300) + "…";
    }
}
