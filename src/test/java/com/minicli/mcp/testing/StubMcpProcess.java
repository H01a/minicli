package com.minicli.mcp.testing;

import com.minicli.mcp.registry.McpServerConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/**
 * 测试专用 MCP stdio server（真实子进程，不作为产品代码）。
 * 支持 initialize / notifications/initialized / tools/list / tools/call(echo、fail)。
 */
public final class StubMcpProcess {

    private StubMcpProcess() {
    }

    public static void main(String[] args) throws IOException {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(
                     new OutputStreamWriter(System.out, StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                JSONObject message = new JSONObject(line.strip());
                if (!message.has("method")) {
                    continue;
                }
                String method = message.getString("method");
                long id = message.optLong("id", -1);
                switch (method) {
                    case "initialize" -> send(out, response(id, new JSONObject()
                            .put("protocolVersion", "2025-06-18")
                            .put("capabilities", new JSONObject().put("tools", new JSONObject()))
                            .put("serverInfo", new JSONObject()
                                    .put("name", "stub")
                                    .put("version", "1.0.0"))));
                    case "tools/list" -> send(out, response(id, new JSONObject().put("tools", tools())));
                    case "tools/call" -> handleCall(out, id, message.optJSONObject("params"));
                    default -> {
                        if (id >= 0) {
                            send(out, error(id, -32601, "Method not found: " + method));
                        }
                    }
                }
            }
        }
    }

    /** 供测试启动：用当前 java + 测试 classpath 拉起本类。 */
    public static McpServerConfig config() {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("java.class.path");
        return new McpServerConfig("stub", java,
                List.of("-cp", classpath, StubMcpProcess.class.getName()));
    }

    private static void handleCall(BufferedWriter out, long id, JSONObject params) throws IOException {
        if (params == null) {
            send(out, error(id, -32602, "Missing params"));
            return;
        }
        String name = params.optString("name", "");
        JSONObject arguments = params.optJSONObject("arguments");
        switch (name) {
            case "echo" -> {
                String message = arguments == null ? "" : arguments.optString("message", "");
                send(out, response(id, toolResult(message, false)));
            }
            case "fail" -> send(out, response(id, toolResult("intentional failure", true)));
            default -> send(out, error(id, -32602, "Unknown tool: " + name));
        }
    }

    private static JSONArray tools() {
        JSONArray tools = new JSONArray();
        tools.put(new JSONObject()
                .put("name", "echo")
                .put("description", "Echo the given message back")
                .put("inputSchema", new JSONObject()
                        .put("type", "object")
                        .put("properties", new JSONObject()
                                .put("message", new JSONObject().put("type", "string")))
                        .put("required", new JSONArray().put("message"))));
        tools.put(new JSONObject()
                .put("name", "fail")
                .put("description", "Always fails")
                .put("inputSchema", new JSONObject()
                        .put("type", "object")
                        .put("properties", new JSONObject())));
        return tools;
    }

    private static JSONObject toolResult(String text, boolean isError) {
        JSONArray content = new JSONArray().put(
                new JSONObject().put("type", "text").put("text", text));
        return new JSONObject().put("content", content).put("isError", isError);
    }

    private static JSONObject response(long id, JSONObject result) {
        return new JSONObject().put("jsonrpc", "2.0").put("id", id).put("result", result);
    }

    private static JSONObject error(long id, int code, String message) {
        return new JSONObject()
                .put("jsonrpc", "2.0")
                .put("id", id)
                .put("error", new JSONObject().put("code", code).put("message", message));
    }

    private static void send(BufferedWriter out, JSONObject message) throws IOException {
        out.write(message.toString());
        out.newLine();
        out.flush();
    }
}
