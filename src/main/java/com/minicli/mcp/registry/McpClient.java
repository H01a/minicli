package com.minicli.mcp.registry;

import com.minicli.mcp.protocol.JsonRpc;
import com.minicli.mcp.protocol.McpException;
import com.minicli.mcp.transport.StdioTransport;
import com.minicli.mcp.transport.Transport;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * MCP 客户端会话：生命周期状态机 + JSON-RPC id 配对。
 * 流程：initialize → notifications/initialized → tools/list（翻页）→ tools/call。
 * 单例线程安全，可被 ReAct 并发调用。
 */
public final class McpClient implements AutoCloseable {

    public static final long DEFAULT_REQUEST_TIMEOUT_MILLIS = 30_000;

    private static final Set<String> SUPPORTED_PROTOCOL_VERSIONS = Set.of(
            JsonRpc.VERSION_2024_11_05,
            JsonRpc.VERSION_2025_03_26,
            JsonRpc.VERSION_2025_06_18);

    public enum State { NEW, INITIALIZING, READY, CLOSED }

    private final McpServerConfig config;
    private final Transport transport;
    private final long requestTimeoutMillis;
    private final AtomicLong nextId = new AtomicLong(1);
    private final Map<Long, CompletableFuture<JSONObject>> pending = new ConcurrentHashMap<>();
    private final AtomicReference<State> state = new AtomicReference<>(State.NEW);

    private volatile List<McpToolSpec> tools = List.of();

    private McpClient(McpServerConfig config, Transport transport, long requestTimeoutMillis) {
        this.config = config;
        this.transport = transport;
        this.requestTimeoutMillis = requestTimeoutMillis;
    }

    public static McpClient connect(McpServerConfig config) {
        return connect(config, DEFAULT_REQUEST_TIMEOUT_MILLIS);
    }

    /** 连接并完成初始化与 tools/list；失败会关闭传输并抛 {@link McpException}。 */
    public static McpClient connect(McpServerConfig config, long requestTimeoutMillis) {
        if (requestTimeoutMillis <= 0) {
            throw new IllegalArgumentException("requestTimeoutMillis 必须为正数");
        }
        McpClient client = new McpClient(config,
                new StdioTransport(config.commandLine()), requestTimeoutMillis);
        client.initialize();
        return client;
    }

    private void initialize() {
        if (!state.compareAndSet(State.NEW, State.INITIALIZING)) {
            throw new McpException("MCP client 已初始化或已关闭");
        }
        try {
            transport.start(new Transport.MessageListener() {
                @Override
                public void onMessage(JSONObject message) {
                    McpClient.this.onMessage(message);
                }

                @Override
                public void onClosed(Throwable error) {
                    McpClient.this.onClosed(error);
                }
            });
        } catch (IOException e) {
            state.set(State.CLOSED);
            throw new McpException("MCP server '" + config.name() + "' 启动失败: " + e.getMessage(), e);
        }
        try {
            JSONObject initResult = JsonRpc.result(request(JsonRpc.request(
                    nextId.getAndIncrement(), "initialize",
                    new JSONObject()
                            .put("protocolVersion", JsonRpc.CLIENT_PROTOCOL_VERSION)
                            .put("capabilities", new JSONObject())
                            .put("clientInfo", new JSONObject()
                                    .put("name", "minicli")
                                    .put("version", "0.1.0")))));
            String serverVersion = initResult.optString("protocolVersion", "");
            if (!SUPPORTED_PROTOCOL_VERSIONS.contains(serverVersion)) {
                throw new McpException("MCP server '" + config.name()
                        + "' 返回不支持的协议版本: " + serverVersion);
            }
            transport.send(JsonRpc.notification("notifications/initialized", new JSONObject()));
            tools = listTools();
            state.set(State.READY);
        } catch (McpException e) {
            state.set(State.CLOSED);
            transport.close();
            throw e;
        } catch (IOException e) {
            state.set(State.CLOSED);
            transport.close();
            throw new McpException("MCP server '" + config.name() + "' 初始化失败: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            state.set(State.CLOSED);
            transport.close();
            throw new McpException("MCP server '" + config.name() + "' 初始化失败: " + e.getMessage(), e);
        }
    }

    /** 已发现工具（MCP 侧原名，未加服务器前缀）。 */
    public List<McpToolSpec> tools() {
        return tools;
    }

    public State state() {
        return state.get();
    }

    /** 调用远端工具；参数为空视为空对象。返回拼接后的文本结果。 */
    public String callTool(String name, JSONObject arguments) {
        ensureReady();
        JSONObject params = new JSONObject().put("name", name);
        if (arguments != null) {
            params.put("arguments", arguments);
        }
        JSONObject result = JsonRpc.result(request(JsonRpc.request(
                nextId.getAndIncrement(), "tools/call", params)));
        String text = extractText(result.optJSONArray("content"));
        if (result.optBoolean("isError", false)) {
            throw new McpException("MCP 工具 '" + name + "' 返回错误: " + text);
        }
        return text;
    }

    private void ensureReady() {
        if (state.get() != State.READY) {
            throw new McpException("MCP server '" + config.name()
                    + "' 未就绪（state=" + state.get() + "）");
        }
    }

    private List<McpToolSpec> listTools() {
        List<McpToolSpec> all = new ArrayList<>();
        String cursor = null;
        while (true) {
            JSONObject params = new JSONObject();
            if (cursor != null) {
                params.put("cursor", cursor);
            }
            JSONObject result = JsonRpc.result(request(JsonRpc.request(
                    nextId.getAndIncrement(), "tools/list", params)));
            JSONArray array = result.optJSONArray("tools");
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    JSONObject tool = array.optJSONObject(i);
                    if (tool == null) {
                        continue;
                    }
                    String name = tool.optString("name", "");
                    if (name.isBlank()) {
                        continue;
                    }
                    String description = tool.optString("description", "");
                    if (description.isBlank()) {
                        description = tool.optString("title", "");
                    }
                    all.add(new McpToolSpec(name, description, tool.optJSONObject("inputSchema")));
                }
            }
            String nextCursor = result.optString("nextCursor", "");
            if (nextCursor == null || nextCursor.isBlank()) {
                return all;
            }
            cursor = nextCursor;
        }
    }

    /** 发送请求并等待同 id 响应；超时/中断/进程退出均转为 McpException。 */
    private JSONObject request(JSONObject requestMessage) {
        long id = JsonRpc.id(requestMessage);
        CompletableFuture<JSONObject> future = new CompletableFuture<>();
        pending.put(id, future);
        try {
            transport.send(requestMessage);
            return future.get(requestTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            pending.remove(id);
            throw new McpException("MCP 请求超时（" + requestTimeoutMillis + "ms）: "
                    + requestMessage.optString("method") + "（server=" + config.name() + "）");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pending.remove(id);
            throw new McpException("MCP 请求被中断: " + requestMessage.optString("method"), e);
        } catch (ExecutionException e) {
            pending.remove(id);
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof McpException mcp) {
                throw mcp;
            }
            throw new McpException("MCP 请求失败: " + requestMessage.optString("method")
                    + "（" + cause.getMessage() + "）", cause);
        } catch (IOException e) {
            pending.remove(id);
            throw new McpException("MCP 发送请求失败: " + requestMessage.optString("method")
                    + "（" + e.getMessage() + "）", e);
        }
    }

    private void onMessage(JSONObject message) {
        if (JsonRpc.isResponse(message)) {
            long id = JsonRpc.id(message);
            CompletableFuture<JSONObject> future = pending.remove(id);
            if (future != null) {
                future.complete(message);
            } else {
                System.err.println("[mcp] 收到未知/过期响应 id=" + id + "（server=" + config.name() + "）");
            }
            return;
        }
        String method = message.optString("method", "");
        if (!method.isBlank()) {
            System.err.println("[mcp] server=" + config.name() + " 发来通知/请求: " + method);
        }
    }

    private void onClosed(Throwable error) {
        String reason = error == null ? "进程已退出/连接关闭" : error.getMessage();
        McpException exception = new McpException("MCP server '" + config.name() + "' " + reason, error);
        failAllPending(exception);
        state.set(State.CLOSED);
    }

    private void failAllPending(McpException exception) {
        for (Map.Entry<Long, CompletableFuture<JSONObject>> entry : pending.entrySet()) {
            entry.getValue().completeExceptionally(exception);
        }
        pending.clear();
    }

    private static String extractText(JSONArray content) {
        if (content == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            JSONObject item = content.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String type = item.optString("type", "");
            if ("text".equals(type)) {
                if (!builder.isEmpty()) {
                    builder.append('\n');
                }
                builder.append(item.optString("text", ""));
            } else {
                if (!builder.isEmpty()) {
                    builder.append('\n');
                }
                builder.append("[MCP content: ").append(type.isBlank() ? "unknown" : type).append(']');
            }
        }
        return builder.toString();
    }

    @Override
    public void close() {
        state.set(State.CLOSED);
        failAllPending(new McpException("MCP client '" + config.name() + "' 已关闭"));
        transport.close();
    }
}
