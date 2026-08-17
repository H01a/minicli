package com.minicli.tools.builtin;

import com.minicli.tools.spi.Tool;
import com.minicli.tools.spi.ToolResult;
import org.json.JSONArray;
import org.json.JSONObject;

/** 返回当前工作目录的绝对路径。只读工具。 */
public final class GetCwdTool implements Tool {

    @Override
    public String name() {
        return "get_cwd";
    }

    @Override
    public String description() {
        return "返回当前工作目录的绝对路径。";
    }

    @Override
    public JSONObject inputSchema() {
        return new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject())
                .put("required", new JSONArray());
    }

    @Override
    public ToolResult invoke(JSONObject args) {
        return ToolResult.success(System.getProperty("user.dir"));
    }
}
