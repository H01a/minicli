package com.minicli.llm;

import com.minicli.config.Config;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
