package com.minicli.mcp.protocol;

import org.json.JSONObject;

/** JSON-RPC 2.0 消息编解码工具（MCP stdio 每行一条 JSON）。 */
public final class JsonRpc {

    public static final String VERSION = "2.0";

    /** 客户端声明使用的 MCP 协议版本；server 可回退到其支持的旧版本。 */
    public static final String CLIENT_PROTOCOL_VERSION = "2025-06-18";
    public static final String VERSION_2024_11_05 = "2024-11-05";
    public static final String VERSION_2025_03_26 = "2025-03-26";
    public static final String VERSION_2025_06_18 = "2025-06-18";

    private JsonRpc() {
    }

    public static JSONObject request(long id, String method, JSONObject params) {
        JSONObject message = new JSONObject()
                .put("jsonrpc", VERSION)
                .put("id", id)
                .put("method", method);
        if (params != null) {
            message.put("params", params);
        }
        return message;
    }

    public static JSONObject notification(String method, JSONObject params) {
        JSONObject message = new JSONObject().put("jsonrpc", VERSION).put("method", method);
        if (params != null) {
            message.put("params", params);
        }
        return message;
    }

    /** 是否为响应：带 id 且有 result 或 error。 */
    public static boolean isResponse(JSONObject message) {
        return message.has("id") && (message.has("result") || message.has("error"));
    }

    /** 读取消息 id（本客户端请求都使用数字 id）。 */
    public static long id(JSONObject message) {
        Object id = message.opt("id");
        if (id instanceof Number number) {
            return number.longValue();
        }
        throw new McpException("JSON-RPC 消息缺少数字 id: " + message);
    }

    /**
     * 从响应里取 result；带 error 时抛 {@link McpException}。
     * 注意：这里接收的是包含 result/error 的完整响应消息。
     */
    public static JSONObject result(JSONObject response) {
        if (!response.has("id")) {
            throw new McpException("非法 JSON-RPC 响应（缺少 id）: " + response);
        }
        if (response.has("error")) {
            JSONObject error = response.optJSONObject("error");
            int code = error == null ? 0 : error.optInt("code", 0);
            String message = error == null ? "未知错误" : error.optString("message", "未知错误");
            throw new McpException("MCP 返回 JSON-RPC 错误 code=" + code + ": " + message);
        }
        JSONObject result = response.optJSONObject("result");
        if (result == null) {
            throw new McpException("JSON-RPC 响应缺少 result/error: " + response);
        }
        return result;
    }
}
