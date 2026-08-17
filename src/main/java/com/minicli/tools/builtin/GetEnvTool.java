package com.minicli.tools.builtin;

import com.minicli.tools.spi.Tool;
import com.minicli.tools.spi.ToolResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 读取环境变量：缺省列出全部变量名，指定 name 时返回其值。只读工具。 */
public final class GetEnvTool implements Tool {

    @Override
    public String name() {
        return "get_env";
    }

    @Override
    public String description() {
        return "读取环境变量：不传 name 时列出全部变量名（按字母序），传 name 时返回其值。";
    }

    @Override
    public JSONObject inputSchema() {
        return new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("name", new JSONObject()
                                .put("type", "string")
                                .put("description", "要读取的环境变量名，缺省列出全部")))
                .put("required", new JSONArray());
    }

    @Override
    public ToolResult invoke(JSONObject args) {
        String name = args.optString("name", "");
        if (name.isBlank()) {
            List<String> names = new ArrayList<>(System.getenv().keySet());
            Collections.sort(names);
            return ToolResult.success(String.join("\n", names));
        }
        String value = System.getenv(name);
        if (value == null) {
            return ToolResult.failure("环境变量不存在: " + name);
        }
        return ToolResult.success(name + "=" + value);
    }
}
