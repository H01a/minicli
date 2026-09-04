package com.minicli.mcp.protocol;

/** MCP 协议、传输或会话运行期错误。 */
public class McpException extends RuntimeException {

    public McpException(String message) {
        super(message);
    }

    public McpException(String message, Throwable cause) {
        super(message, cause);
    }
}
