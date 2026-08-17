package com.minicli.llm;

import com.minicli.config.Config;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeepSeekClientTest {

    private MockWebServer server;
    private DeepSeekClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        Config config = Config.of("test-key", "deepseek-v4-flash", server.url("/").toString());
        client = new DeepSeekClient(config);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void askSendsStreamRequest() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseDelta("Hello!")));

        String answer = client.ask("Hi");

        assertEquals("Hello!", answer);
        RecordedRequest request = server.takeRequest();
        assertEquals("/responses", request.getPath());
        assertEquals("Bearer test-key", request.getHeader("Authorization"));
        assertTrue(request.getHeader("Accept").contains("text/event-stream"));
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"model\":\"deepseek-v4-flash\""));
        assertTrue(body.contains("\"input\":\"Hi\""));
        assertTrue(body.contains("\"stream\":true"));
    }

    @Test
    void askStreamDeliversDeltasInOrder() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseDeltas(List.of("Hel", "lo!", " 我是 DeepSeek"))));

        List<String> deltas = new ArrayList<>();
        boolean[] done = {false};
        client.askStream("Hi", new StreamHandler() {
            @Override
            public void onOutputDelta(String delta) {
                deltas.add(delta);
            }

            @Override
            public void onDone() {
                done[0] = true;
            }
        });

        assertEquals(List.of("Hel", "lo!", " 我是 DeepSeek"), deltas);
        assertTrue(done[0], "response.completed 后应回调 onDone");
    }

    @Test
    void askFailsOnHttpError() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"error\":\"unauthorized\"}"));

        LlmException ex = assertThrows(LlmException.class, () -> client.ask("Hi"));

        assertTrue(ex.getMessage().contains("401"));
    }

    @Test
    void askStreamFailsOnFailedEvent() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("""
                        event: response.failed
                        data: {"type":"response.failed","sequence_number":1,"response":{"id":"r","object":"response","status":"failed","error":{"code":"api_error","message":"boom"}}}

                        """));

        LlmException ex = assertThrows(LlmException.class, () -> client.ask("Hi"));

        assertTrue(ex.getMessage().contains("boom"));
    }

    @Test
    void askStreamFailsOnIncompleteEvent() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("""
                        event: response.incomplete
                        data: {"type":"response.incomplete","sequence_number":1,"response":{"id":"r","object":"response","status":"incomplete","incomplete_details":{"reason":"max_output_tokens"}}}

                        """));

        assertThrows(LlmException.class, () -> client.ask("Hi"));
    }

    @Test
    void askAgentSendsToolsAndInputArray() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseDelta("Hello!")));

        LlmTurnResult result = client.askAgent(
                List.of(new JSONObject().put("type", "message").put("role", "user").put("content", "Hi")),
                List.of(new JSONObject().put("type", "function").put("name", "echo")));

        assertTrue(result.finished());
        assertEquals("Hello!", result.outputText());
        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"tools\""));
        assertTrue(body.contains("\"tool_choice\":\"auto\""));
        assertTrue(body.contains("\"type\":\"message\""));
    }

    @Test
    void askAgentParsesFunctionCallFromCompleted() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sseFunctionCall("call_9", "echo", new JSONObject().put("text", "hi"))));

        LlmTurnResult result = client.askAgent(List.of(), List.of());

        assertFalse(result.finished());
        assertEquals(1, result.functionCalls().size());
        assertEquals("call_9", result.functionCalls().get(0).callId());
        assertEquals("echo", result.functionCalls().get(0).name());
        assertEquals("{\"text\":\"hi\"}", result.functionCalls().get(0).argumentsJson());
    }

    private static String sseDelta(String text) {
        return """
                event: response.output_text.delta
                data: {"type":"response.output_text.delta","sequence_number":1,"item_id":"msg_1","output_index":0,"content_index":0,"delta":"%s"}

                event: response.completed
                data: {"type":"response.completed","sequence_number":2,"response":{"id":"resp_1","object":"response","status":"completed","model":"deepseek-v4-flash","output":[],"usage":{}}}

                """.formatted(text);
    }

    private static String sseDeltas(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        int seq = 0;
        for (String part : parts) {
            seq++;
            sb.append("event: response.output_text.delta\n");
            sb.append("data: {\"type\":\"response.output_text.delta\",\"sequence_number\":").append(seq)
                    .append(",\"item_id\":\"msg_1\",\"output_index\":0,\"content_index\":0,\"delta\":\"")
                    .append(part).append("\"}\n\n");
        }
        seq++;
        sb.append("event: response.completed\n");
        sb.append("data: {\"type\":\"response.completed\",\"sequence_number\":").append(seq)
                .append(",\"response\":{\"id\":\"resp_1\",\"object\":\"response\",\"status\":\"completed\",\"model\":\"deepseek-v4-flash\",\"output\":[],\"usage\":{}}}\n\n");
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

    /*
     * 用户手动真实验证用（调用真实 DeepSeek API，依赖 .env 有效密钥）。
     * 暂注释保留、不删除；需要时取消注释即可。
     */
    // @Test
    // void testStream(){
    //     Config config = Config.load();
    //     DeepSeekClient client = new DeepSeekClient(config);
    //     client.askStream("say: 'mamba out' 100times", new StreamHandler() {
    //         @Override
    //         public void onOutputDelta(String delta) {
    //             System.out.println(delta);
    //         }
    //
    //         @Override
    //         public void onDone() {
    //             System.out.println("说完了");
    //         }
    //     });
    // }
}
