package com.minicli.mcp.registry;

import java.util.ArrayList;
import java.util.List;

/** MCP stdio server 配置（config/mcp-servers.json 中的一条记录）。 */
public record McpServerConfig(String name, String command, List<String> args) {

    private static final String NAME_PATTERN = "[A-Za-z0-9_-]+";

    public McpServerConfig {
        if (name == null || !name.matches(NAME_PATTERN)) {
            throw new IllegalArgumentException("MCP server name 非法（仅允许字母/数字/_/-）: " + name);
        }
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("MCP server " + name + " 缺少 command");
        }
        args = args == null ? List.of() : List.copyOf(args);
    }

    /** 组装 ProcessBuilder 用的完整命令行。 */
    public List<String> commandLine() {
        List<String> all = new ArrayList<>(1 + args.size());
        all.add(command);
        all.addAll(args);
        return List.copyOf(all);
    }
}
