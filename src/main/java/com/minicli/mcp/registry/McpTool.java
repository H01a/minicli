package com.minicli.mcp.registry;

import com.minicli.mcp.protocol.McpException;
import com.minicli.tools.spi.Tool;
import com.minicli.tools.spi.ToolResult;
import org.json.JSONObject;

/** 把远端 MCP 工具适配为统一 Tool：对外暴露名加 server 前缀，调用时去掉前缀转发。 */
public final class McpTool implements Tool {

    private final McpClient client;
    private final String exposedName;
    private final McpToolSpec spec;

    public McpTool(McpClient client, String serverName, McpToolSpec spec) {
        this.client = client;
        this.exposedName = serverName + "_" + spec.name();
        this.spec = spec;
    }

    @Override
    public String name() {
        return exposedName;
    }

    @Override
    public String description() {
        return spec.description();
    }

    @Override
    public JSONObject inputSchema() {
        return spec.inputSchema();
    }

    @Override
    public ToolResult invoke(JSONObject args) {
        try {
            return ToolResult.success(client.callTool(spec.name(), args));
        } catch (McpException e) {
            return ToolResult.failure("MCP 工具 " + exposedName + " 调用失败: " + e.getMessage());
        } catch (RuntimeException e) {
            return ToolResult.failure("MCP 工具 " + exposedName + " 调用异常: " + e.getMessage());
        }
    }
}
