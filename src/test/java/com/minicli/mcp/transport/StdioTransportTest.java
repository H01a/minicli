package com.minicli.mcp.transport;

import com.minicli.mcp.protocol.JsonRpc;
import com.minicli.mcp.testing.StubMcpProcess;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StdioTransportTest {

    @Test
    void startFailsWithClearMessageWhenCommandMissing() {
        StdioTransport transport = new StdioTransport(List.of("definitely-missing-mcp-server-xyz"));

        IOException ex = assertThrows(IOException.class,
                () -> transport.start(new Transport.MessageListener() {
                    @Override
                    public void onMessage(JSONObject message) {
                    }

                    @Override
                    public void onClosed(Throwable error) {
                    }
                }));

        assertTrue(ex.getMessage().contains("启动 MCP server 失败"));
    }

    @Test
    void newlineJsonRoundTripOverRealSubprocess() throws Exception {
        StdioTransport transport = new StdioTransport(StubMcpProcess.config().commandLine());
        CountDownLatch latch = new CountDownLatch(1);
        List<JSONObject> received = new CopyOnWriteArrayList<>();
        transport.start(new Transport.MessageListener() {
            @Override
            public void onMessage(JSONObject message) {
                received.add(message);
                latch.countDown();
            }

            @Override
            public void onClosed(Throwable error) {
            }
        });

        transport.send(JsonRpc.request(1, "initialize", new JSONObject()));

        assertTrue(latch.await(10, TimeUnit.SECONDS), "10s 内未收到 stub server 响应");
        assertEquals(1, received.size());
        assertEquals(1L, JsonRpc.id(received.get(0)));
        transport.close();
    }
}
