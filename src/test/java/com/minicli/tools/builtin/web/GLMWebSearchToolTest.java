package com.minicli.tools.builtin.web;

import com.minicli.tools.spi.ToolResult;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GLMWebSearchToolTest {

    private MockWebServer server;
    private GLMWebSearchTool tool;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        tool = new GLMWebSearchTool("test-glm-key",
                server.url("/api/paas/v4/web_search").toString(),
                new OkHttpClient.Builder()
                        .connectTimeout(Duration.ofSeconds(2))
                        .readTimeout(Duration.ofSeconds(5))
                        .build());
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void sendsRequestAndParsesResults() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"id":"x","created":1,"request_id":"r","search_result":[{"title":"Java 21 发布","content":"新特性摘要","link":"https://example.com/java21","media":"示例站","publish_date":"2026-09-01"}]}
                        """));

        ToolResult result = tool.invoke(new JSONObject().put("search_query", "Java 21"));

        assertTrue(result.isSuccess());
        assertTrue(result.output().contains("Java 21 发布"));
        assertTrue(result.output().contains("https://example.com/java21"));
        RecordedRequest request = server.takeRequest();
        assertEquals("/api/paas/v4/web_search", request.getPath());
        assertEquals("Bearer test-glm-key", request.getHeader("Authorization"));
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"search_query\":\"Java 21\""));
        assertTrue(body.contains("\"search_engine\":\"search_std\""));
        assertTrue(body.contains("\"count\":10"));
        assertTrue(body.contains("\"search_recency_filter\":\"noLimit\""));
    }

    @Test
    void missingQueryFails() {
        ToolResult result = tool.invoke(new JSONObject());
        assertFalse(result.isSuccess());
        assertTrue(result.error().contains("缺少必填参数 search_query"));
    }

    @Test
    void queryLongerThan70Fails() {
        ToolResult result = tool.invoke(new JSONObject().put("search_query", "x".repeat(71)));
        assertFalse(result.isSuccess());
        assertTrue(result.error().contains("不能超过 70"));
    }

    @Test
    void countOutOfRangeFails() {
        ToolResult result = tool.invoke(new JSONObject()
                .put("search_query", "java")
                .put("count", 51));
        assertFalse(result.isSuccess());
        assertTrue(result.error().contains("count 需在 1..50"));
    }

    @Test
    void invalidEngineFails() {
        ToolResult result = tool.invoke(new JSONObject()
                .put("search_query", "java")
                .put("search_engine", "bogus"));
        assertFalse(result.isSuccess());
        assertTrue(result.error().contains("无效的 search_engine"));
    }

    @Test
    void httpErrorFails() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("{\"error\":{\"code\":\"unauthorized\",\"message\":\"bad key\"}}"));

        ToolResult result = tool.invoke(new JSONObject().put("search_query", "java"));

        assertFalse(result.isSuccess());
        assertTrue(result.error().contains("HTTP 401"));
    }

    @Test
    void apiErrorFails() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":{\"code\":\"rate_limit\",\"message\":\"too many\"}}"));

        ToolResult result = tool.invoke(new JSONObject().put("search_query", "java"));

        assertFalse(result.isSuccess());
        assertTrue(result.error().contains("too many"));
    }

    @Test
    void emptyResultsReturnsNoMatch() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"search_result\":[]}"));

        ToolResult result = tool.invoke(new JSONObject().put("search_query", "java"));

        assertTrue(result.isSuccess());
        assertTrue(result.output().contains("无搜索结果"));
    }

    @Test
    void missingApiKeyFails() {
        GLMWebSearchTool noKeyTool = new GLMWebSearchTool("",
                server.url("/api/paas/v4/web_search").toString(),
                new OkHttpClient());
        ToolResult result = noKeyTool.invoke(new JSONObject().put("search_query", "java"));
        assertFalse(result.isSuccess());
        assertTrue(result.error().contains("未配置 GLM_API_KEY"));
    }
}
