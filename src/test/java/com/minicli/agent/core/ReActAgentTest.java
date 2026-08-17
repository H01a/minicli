package com.minicli.agent.core;

import com.minicli.config.Config;
import com.minicli.llm.DeepSeekClient;
import com.minicli.tools.spi.Tool;
import com.minicli.tools.spi.ToolRegistry;
import com.minicli.tools.spi.ToolResult;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReActAgentTest {

    private MockWebServer server;
    private ToolRegistry registry;
    private ReActAgent agent;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        Config config = Config.of("test-key", "deepseek-v4-flash", server.url("/").toString());
        registry = new ToolRegistry();
        registry.register(echoTool());
        agent = new ReActAgent(new DeepSeekClient(config), registry);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void runsToolCallThenFinalAnswer() throws Exception {
        server.enqueue(ok(sseFunctionCall("call_1", "echo", new JSONObject().put("text", "hi"))));
        server.enqueue(ok(sseText("好的，回显 hi")));

        String answer = agent.run("回显 hi");

        assertEquals("好的，回显 hi", answer);
        assertEquals(2, server.getRequestCount());
        server.takeRequest();
        RecordedRequest second = server.takeRequest();
        String body2 = second.getBody().readUtf8();
        assertTrue(body2.contains("\"type\":\"function_call\""));
        assertTrue(body2.contains("\"call_id\":\"call_1\""));
        assertTrue(body2.contains("\"type\":\"function_call_output\""));
        assertTrue(body2.contains("echo:hi"));
    }

    @Test
    void answersDirectlyWithoutToolCall() throws Exception {
        server.enqueue(ok(sseText("直接回答")));

        String answer = agent.run("你好");

        assertEquals("直接回答", answer);
        assertEquals(1, server.getRequestCount());
    }

    @Test
    void unknownToolFeedsBackFailure() throws Exception {
        server.enqueue(ok(sseFunctionCall("call_1", "nope", new JSONObject())));
        server.enqueue(ok(sseText("这个工具不存在")));

        String answer = agent.run("调一个不存在的工具");

        assertEquals("这个工具不存在", answer);
        server.takeRequest();
        RecordedRequest second = server.takeRequest();
        assertTrue(second.getBody().readUtf8().contains("未注册的工具"));
    }

    @Test
    void exceedsMaxStepsThrows() throws Exception {
        ReActAgent tiny = new ReActAgent(new DeepSeekClient(
                Config.of("test-key", "deepseek-v4-flash", server.url("/").toString())), registry, 2);
        server.enqueue(ok(sseFunctionCall("call_1", "echo", new JSONObject().put("text", "a"))));
        server.enqueue(ok(sseFunctionCall("call_2", "echo", new JSONObject().put("text", "b"))));

        assertThrows(AgentException.class, () -> tiny.run("循环"));

        assertEquals(2, server.getRequestCount());
    }

    @Test
    void passesBackReasoningInThinkingMode() throws Exception {
        server.enqueue(ok(sseReasoningWithFunctionCall("call_1", "echo", new JSONObject().put("text", "hi"))));
        server.enqueue(ok(sseText("最终回答")));

        String answer = agent.run("思考");

        assertEquals("最终回答", answer);
        server.takeRequest();
        RecordedRequest second = server.takeRequest();
        String body = second.getBody().readUtf8();
        assertTrue(body.contains("\"type\":\"reasoning\""));
        assertTrue(body.contains("\"type\":\"reasoning_text\""));
        assertTrue(body.contains("thinking..."));
    }

    @Test
    void addsReasoningBeforeEachParallelCall() throws Exception {
        server.enqueue(ok(sseReasoningWithFunctionCalls(List.of(
                new String[]{"call_1", "echo", new JSONObject().toString()},
                new String[]{"call_2", "echo", new JSONObject().toString()}))));
        server.enqueue(ok(sseText("完成")));

        agent.run("并行");

        server.takeRequest();
        RecordedRequest second = server.takeRequest();
        String body = second.getBody().readUtf8();
        long reasoningItems = body.split("\"type\":\"reasoning\"", -1).length - 1;
        assertEquals(2, reasoningItems, "每个 function_call 前都应有一个 reasoning item");
    }

    @Test
    void executesCallsInParallel() throws Exception {
        CountDownLatch arrived = new CountDownLatch(2);
        registry.register(barrierTool("t1", arrived));
        registry.register(barrierTool("t2", arrived));

        server.enqueue(ok(sseFunctionCalls(List.of(
                new String[]{"call_1", "t1", new JSONObject().toString()},
                new String[]{"call_2", "t2", new JSONObject().toString()}))));
        server.enqueue(ok(sseText("并行完成")));

        String answer = agent.run("并行");

        assertEquals("并行完成", answer);
        server.takeRequest();
        RecordedRequest second = server.takeRequest();
        assertTrue(second.getBody().readUtf8().contains("both"), "两个调用应同时进入，串行执行会超时");
    }

    @Test
    void capsConcurrencyAtFour() throws Exception {
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        List<String[]> calls = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String name = "c" + i;
            registry.register(concurrentTool(name, active, maxActive));
            calls.add(new String[]{"call_" + i, name, new JSONObject().toString()});
        }
        server.enqueue(ok(sseFunctionCalls(calls)));
        server.enqueue(ok(sseText("上限达标")));

        String answer = agent.run("并发上限");

        assertEquals("上限达标", answer);
        assertEquals(4, maxActive.get());
        server.takeRequest();
        RecordedRequest second = server.takeRequest();
        assertTrue(second.getBody().readUtf8().contains("\"output\":\"done\""));
    }

    private static Tool barrierTool(String name, CountDownLatch arrived) {
        return new Tool() {
            @Override public String name() { return name; }
            @Override public String description() { return "等待两个调用同时到达"; }
            @Override public JSONObject inputSchema() { return new JSONObject(); }
            @Override public ToolResult invoke(JSONObject args) {
                arrived.countDown();
                try {
                    return arrived.await(2, TimeUnit.SECONDS)
                            ? ToolResult.success("both")
                            : ToolResult.failure("timeout");
                } catch (InterruptedException e) {
                    return ToolResult.failure("interrupted");
                }
            }
        };
    }

    private static Tool concurrentTool(String name, AtomicInteger active, AtomicInteger maxActive) {
        return new Tool() {
            @Override public String name() { return name; }
            @Override public String description() { return "并发计数工具"; }
            @Override public JSONObject inputSchema() { return new JSONObject(); }
            @Override public ToolResult invoke(JSONObject args) {
                int cur = active.incrementAndGet();
                maxActive.accumulateAndGet(cur, Math::max);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                active.decrementAndGet();
                return ToolResult.success("done");
            }
        };
    }

    private static String sseFunctionCalls(List<String[]> calls) {
        StringBuilder sb = new StringBuilder();
        StringBuilder output = new StringBuilder("[");
        int seq = 0;
        for (int i = 0; i < calls.size(); i++) {
            String[] c = calls.get(i);
            seq++;
            sb.append("event: response.output_item.done\n");
            sb.append("data: {\"type\":\"response.output_item.done\",\"sequence_number\":").append(seq)
                    .append(",\"item\":{\"type\":\"function_call\",\"id\":\"fc_").append(i + 1)
                    .append("\",\"call_id\":\"").append(c[0])
                    .append("\",\"name\":\"").append(c[1])
                    .append("\",\"arguments\":").append(JSONObject.quote(c[2])).append("}}\n\n");
            if (i > 0) {
                output.append(",");
            }
            output.append("{\"type\":\"function_call\",\"id\":\"fc_").append(i + 1)
                    .append("\",\"call_id\":\"").append(c[0])
                    .append("\",\"name\":\"").append(c[1])
                    .append("\",\"arguments\":").append(JSONObject.quote(c[2])).append("}");
        }
        output.append("]");
        seq++;
        sb.append("event: response.completed\n");
        sb.append("data: {\"type\":\"response.completed\",\"sequence_number\":").append(seq)
                .append(",\"response\":{\"id\":\"resp_1\",\"object\":\"response\",\"status\":\"completed\",\"model\":\"deepseek-v4-flash\",\"output\":").append(output).append(",\"usage\":{}}}\n\n");
        return sb.toString();
    }

    private static Tool echoTool() {
        return new Tool() {
            @Override public String name() { return "echo"; }
            @Override public String description() { return "回显 text 参数"; }
            @Override public JSONObject inputSchema() {
                return new JSONObject()
                        .put("type", "object")
                        .put("properties", new JSONObject()
                                .put("text", new JSONObject().put("type", "string")))
                        .put("required", new JSONArray().put("text"));
            }
            @Override public ToolResult invoke(JSONObject args) {
                return ToolResult.success("echo:" + args.optString("text"));
            }
        };
    }

    private static MockResponse ok(String body) {
        return new MockResponse().setHeader("Content-Type", "text/event-stream").setBody(body);
    }

    private static String sseText(String text) {
        return """
                event: response.output_text.delta
                data: {"type":"response.output_text.delta","sequence_number":1,"item_id":"msg_1","output_index":0,"content_index":0,"delta":"%s"}

                event: response.completed
                data: {"type":"response.completed","sequence_number":2,"response":{"id":"resp_1","object":"response","status":"completed","model":"deepseek-v4-flash","output":[],"usage":{}}}

                """.formatted(text);
    }

    private static String sseReasoningWithFunctionCall(String callId, String name, JSONObject arguments) {
        String args = arguments.toString();
        String quoted = JSONObject.quote(args);
        return """
                event: response.reasoning_text.delta
                data: {"type":"response.reasoning_text.delta","sequence_number":1,"item_id":"rs_1","output_index":0,"content_index":0,"delta":"thinking..."}

                event: response.output_item.done
                data: {"type":"response.output_item.done","sequence_number":2,"item":{"type":"function_call","id":"fc_1","call_id":"%s","name":"%s","arguments":%s}}

                event: response.completed
                data: {"type":"response.completed","sequence_number":3,"response":{"id":"resp_1","object":"response","status":"completed","model":"deepseek-v4-flash","output":[{"type":"function_call","id":"fc_1","call_id":"%s","name":"%s","arguments":%s}],"usage":{}}}

                """.formatted(callId, name, quoted, callId, name, quoted);
    }

    private static String sseReasoningWithFunctionCalls(List<String[]> calls) {
        StringBuilder sb = new StringBuilder();
        StringBuilder output = new StringBuilder("[");
        sb.append("event: response.reasoning_text.delta\n");
        sb.append("data: {\"type\":\"response.reasoning_text.delta\",\"sequence_number\":1,\"item_id\":\"rs_1\",\"output_index\":0,\"content_index\":0,\"delta\":\"thinking...\"}\n\n");
        int seq = 1;
        for (int i = 0; i < calls.size(); i++) {
            String[] c = calls.get(i);
            seq++;
            sb.append("event: response.output_item.done\n");
            sb.append("data: {\"type\":\"response.output_item.done\",\"sequence_number\":").append(seq)
                    .append(",\"item\":{\"type\":\"function_call\",\"id\":\"fc_").append(i + 1)
                    .append("\",\"call_id\":\"").append(c[0])
                    .append("\",\"name\":\"").append(c[1])
                    .append("\",\"arguments\":").append(JSONObject.quote(c[2])).append("}}\n\n");
            if (i > 0) {
                output.append(",");
            }
            output.append("{\"type\":\"function_call\",\"id\":\"fc_").append(i + 1)
                    .append("\",\"call_id\":\"").append(c[0])
                    .append("\",\"name\":\"").append(c[1])
                    .append("\",\"arguments\":").append(JSONObject.quote(c[2])).append("}");
        }
        output.append("]");
        seq++;
        sb.append("event: response.completed\n");
        sb.append("data: {\"type\":\"response.completed\",\"sequence_number\":").append(seq)
                .append(",\"response\":{\"id\":\"resp_1\",\"object\":\"response\",\"status\":\"completed\",\"model\":\"deepseek-v4-flash\",\"output\":").append(output).append(",\"usage\":{}}}\n\n");
        return sb.toString();
    }

    private static String sseFunctionCall(String callId, String name, JSONObject arguments) {
        String args = arguments.toString();
        String quoted = JSONObject.quote(args);
        return """
                event: response.output_item.done
                data: {"type":"response.output_item.done","sequence_number":1,"item":{"type":"function_call","id":"fc_1","call_id":"%s","name":"%s","arguments":%s}}

                event: response.completed
                data: {"type":"response.completed","sequence_number":2,"response":{"id":"resp_1","object":"response","status":"completed","model":"deepseek-v4-flash","output":[{"type":"function_call","id":"fc_1","call_id":"%s","name":"%s","arguments":%s}],"usage":{}}}

                """.formatted(callId, name, quoted, callId, name, quoted);
    }
}
