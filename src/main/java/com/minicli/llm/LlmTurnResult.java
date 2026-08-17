package com.minicli.llm;

import java.util.List;

/** LLM 单轮响应的结构化结果：累积文本 + 本轮发出的工具调用。 */
public record LlmTurnResult(String outputText, String reasoningText, List<FunctionCall> functionCalls) {

    /** 本轮是否以最终回答结束（没有工具调用）。 */
    public boolean finished() {
        return functionCalls.isEmpty();
    }
}
