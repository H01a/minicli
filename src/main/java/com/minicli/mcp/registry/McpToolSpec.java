package com.minicli.mcp.registry;

import org.json.JSONObject;

/** tools/list 返回的单个 MCP 工具（name 为 server 侧原名，未加前缀）。 */
public record McpToolSpec(String name, String description, JSONObject inputSchema) {

    public McpToolSpec {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("MCP 工具名不能为空");
        }
        description = description == null ? "" : description;
        inputSchema = inputSchema == null ? new JSONObject() : inputSchema;
    }
}
