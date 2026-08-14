package com.minicli.llm;

/** LLM 调用失败（网络错误、API 错误、响应解析失败）。 */
public class LlmException extends RuntimeException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
