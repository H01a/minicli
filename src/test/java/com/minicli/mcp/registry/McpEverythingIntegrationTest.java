package com.minicli.mcp.registry;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * everything 官方 server 冒烟测试：默认跳过，仅在
 * MINICLI_MCP_EVERYTHING_IT=true 时执行（首次运行会下载 npm 包）。
 */
@EnabledIfEnvironmentVariable(named = "MINICLI_MCP_EVERYTHING_IT", matches = "true")
class McpEverythingIntegrationTest {

    @Test
    void initializeListAndEchoThroughEverything() {
        McpServerConfig config = new McpServerConfig("everything", "npx",
                List.of("-y", "@modelcontextprotocol/server-everything"));

        try (McpClient client = McpClient.connect(config, 120_000)) {
            assertTrue(client.tools().stream().anyMatch(spec -> spec.name().equals("echo")));
            String output = client.callTool("echo", new JSONObject().put("message", "hello from minicli"));
            assertEquals("Echo: hello from minicli", output);
        }
    }
}
