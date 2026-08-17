package com.minicli.tools.spi;

import org.json.JSONObject;

/**
 * 统一工具抽象：name/description/inputSchema 是给 LLM 的"说明书"，
 * invoke 是真正干活的方法。MCP 工具将来实现同一接口注册进 ToolRegistry。
 */
public interface Tool {

    /** 工具唯一名，LLM Function Calling 用它指定调用哪个工具。 */
    String name();

    /** 工具用途说明（给 LLM 看的）。 */
    String description();

    /** 入参 JSON Schema（给 LLM 描述参数结构）。 */
    JSONObject inputSchema();

    /** 执行工具：args 为 LLM 填写的参数；实现必须把任何异常转换为 FAILURE，不得抛给上层。 */
    ToolResult invoke(JSONObject args);
}
