package com.minicli.mcp.registry;

import com.minicli.mcp.testing.StubMcpProcess;
import com.minicli.tools.spi.Tool;
import com.minicli.tools.spi.ToolRegistry;
import com.minicli.tools.spi.ToolResult;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolTest {

    private static McpToolSpec specNamed(McpClient client, String name) {
        return client.tools().stream()
                .filter(spec -> spec.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void adapterRegistersWithServerPrefixAndInvokesRemoteEcho() {
        try (McpClient client = McpClient.connect(StubMcpProcess.config())) {
            Tool tool = new McpTool(client, "stub", specNamed(client, "echo"));
            assertEquals("stub_echo", tool.name());

            ToolRegistry registry = new ToolRegistry();
            registry.register(tool);

            ToolResult result = registry.require("stub_echo")
                    .invoke(new JSONObject().put("message", "hi"));

            assertTrue(result.isSuccess());
            assertEquals("hi", result.output());
        }
    }

    @Test
    void remoteFailureBecomesToolFailure() {
        try (McpClient client = McpClient.connect(StubMcpProcess.config())) {
            Tool tool = new McpTool(client, "stub", specNamed(client, "fail"));

            ToolResult result = tool.invoke(new JSONObject());

            assertFalse(result.isSuccess());
            assertTrue(result.error().contains("intentional failure"));
        }
    }

    @Test
    void duplicatePrefixedNameRejectedByRegistry() {
        try (McpClient client = McpClient.connect(StubMcpProcess.config())) {
            ToolRegistry registry = new ToolRegistry();
            registry.register(new McpTool(client, "stub", specNamed(client, "echo")));

            assertThrows(IllegalArgumentException.class,
                    () -> registry.register(new McpTool(client, "stub", specNamed(client, "echo"))));
        }
    }
}
