package com.minicli.mcp.registry;

import com.minicli.mcp.protocol.McpException;
import com.minicli.mcp.testing.StubMcpProcess;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpClientTest {

    @Test
    void connectDiscoversToolsAndReachesReady() {
        try (McpClient client = McpClient.connect(StubMcpProcess.config())) {
            assertEquals(McpClient.State.READY, client.state());
            assertTrue(client.tools().stream().anyMatch(spec -> spec.name().equals("echo")));
            assertTrue(client.tools().stream().anyMatch(spec -> spec.name().equals("fail")));
            McpToolSpec echo = client.tools().stream()
                    .filter(spec -> spec.name().equals("echo"))
                    .findFirst()
                    .orElseThrow();
            assertNotNull(echo.inputSchema().optJSONObject("properties"));
        }
    }

    @Test
    void echoToolRoundTrip() {
        try (McpClient client = McpClient.connect(StubMcpProcess.config())) {
            String output = client.callTool("echo", new JSONObject().put("message", "hello"));
            assertEquals("hello", output);
        }
    }

    @Test
    void toolIsErrorThrows() {
        try (McpClient client = McpClient.connect(StubMcpProcess.config())) {
            McpException ex = assertThrows(McpException.class,
                    () -> client.callTool("fail", new JSONObject()));
            assertTrue(ex.getMessage().contains("intentional failure"));
        }
    }

    @Test
    void unknownToolReturnsJsonRpcError() {
        try (McpClient client = McpClient.connect(StubMcpProcess.config())) {
            McpException ex = assertThrows(McpException.class,
                    () -> client.callTool("nope", new JSONObject()));
            assertTrue(ex.getMessage().contains("code=-32602"));
        }
    }

    @Test
    void closeIsIdempotent() {
        McpClient client = McpClient.connect(StubMcpProcess.config());
        client.close();
        client.close();
        assertEquals(McpClient.State.CLOSED, client.state());
    }
}
