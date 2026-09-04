package com.minicli.mcp.transport;

import org.json.JSONObject;

import java.io.IOException;

/**
 * MCP 传输层抽象：M3 由 {@link StdioTransport} 实现（子进程 stdin/stdout），
 * M4 Streamable HTTP 再实现一个版本，会话层不感知具体传输。
 */
public interface Transport extends AutoCloseable {

    /** 启动传输并开始接收消息；实现必须保证只调用一次。 */
    void start(MessageListener listener) throws IOException;

    /** 发送一条 JSON-RPC 消息（stdio 下实现负责补换行）。 */
    void send(JSONObject message) throws IOException;

    /** 关闭传输（幂等）。 */
    @Override
    void close();

    interface MessageListener {

        /** 收到一行可解析的 JSON-RPC 消息。 */
        void onMessage(JSONObject message);

        /** 连接关闭或读取出错；error 为 null 表示正常 EOF。 */
        void onClosed(Throwable error);
    }
}
