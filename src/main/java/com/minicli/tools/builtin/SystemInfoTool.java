package com.minicli.tools.builtin;

import com.minicli.tools.spi.Tool;
import com.minicli.tools.spi.ToolResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.ZoneId;

/** 返回系统与 JDK 基本信息。只读工具。 */
public final class SystemInfoTool implements Tool {

    @Override
    public String name() {
        return "system_info";
    }

    @Override
    public String description() {
        return "返回系统与 JDK 基本信息（OS/架构/JDK/用户/时区/处理器数）。";
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
        return ToolResult.success("""
                os=%s %s (%s)
                java=%s (%s)
                user=%s
                home=%s
                cwd=%s
                timezone=%s
                processors=%d
                """.formatted(
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"),
                System.getProperty("java.version"),
                System.getProperty("java.vendor"),
                System.getProperty("user.name"),
                System.getProperty("user.home"),
                System.getProperty("user.dir"),
                ZoneId.systemDefault().getId(),
                Runtime.getRuntime().availableProcessors()));
    }
}
