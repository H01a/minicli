package com.minicli.llm;

/** 模型输出的一个函数调用：callId 用于关联后续 function_call_output，arguments 是 JSON 字符串。 */
public record FunctionCall(String callId, String name, String argumentsJson) {
}
