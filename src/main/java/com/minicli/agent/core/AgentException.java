package com.minicli.agent.core;

/** Agent 主循环失败（如超过最大步数仍未得到最终回答）。 */
public class AgentException extends RuntimeException {

    public AgentException(String message) {
        super(message);
    }

    public AgentException(String message, Throwable cause) {
        super(message, cause);
    }
}
