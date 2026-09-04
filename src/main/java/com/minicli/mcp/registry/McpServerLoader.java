package com.minicli.mcp.registry;

import com.minicli.mcp.protocol.McpException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 加载 MCP server 清单 JSON：
 * <pre>
 * { "servers": [ { "name": "everything", "command": "npx",
 *                   "args": ["-y", "@modelcontextprotocol/server-everything"] } ] }
 * </pre>
 * 文件不存在视为未启用 MCP，返回空列表。
 */
public final class McpServerLoader {

    private McpServerLoader() {
    }

    public static List<McpServerConfig> load(Path file) throws IOException {
        if (file == null || !Files.exists(file)) {
            return List.of();
        }
        String content;
        try {
            content = Files.readString(file);
        } catch (IOException e) {
            throw new IOException("读取 MCP 配置失败: " + file + "（" + e.getMessage() + "）", e);
        }
        JSONObject root;
        try {
            root = new JSONObject(content);
        } catch (JSONException e) {
            throw new McpException("MCP 配置文件不是合法 JSON: " + file + "（" + e.getMessage() + "）");
        }
        JSONArray array = root.optJSONArray("servers");
        if (array == null) {
            throw new McpException("MCP 配置文件缺少 servers 数组: " + file);
        }
        List<McpServerConfig> servers = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.optJSONObject(i);
            if (object == null) {
                throw new McpException("MCP 配置文件第 " + (i + 1) + " 个 server 不是 JSON 对象: " + file);
            }
            String name = object.optString("name", "");
            String command = object.optString("command", "");
            List<String> args = new ArrayList<>();
            JSONArray argsArray = object.optJSONArray("args");
            if (argsArray != null) {
                for (int j = 0; j < argsArray.length(); j++) {
                    Object arg = argsArray.opt(j);
                    if (!(arg instanceof String text)) {
                        throw new McpException("MCP server " + name + " 的 args 第 " + (j + 1)
                                + " 项不是字符串: " + file);
                    }
                    args.add(text);
                }
            }
            McpServerConfig config;
            try {
                config = new McpServerConfig(name, command, args);
            } catch (IllegalArgumentException e) {
                throw new McpException("MCP server 配置非法（" + e.getMessage() + "）: " + file);
            }
            if (!names.add(config.name())) {
                throw new McpException("MCP server name 重复: " + config.name());
            }
            servers.add(config);
        }
        return servers;
    }
}
