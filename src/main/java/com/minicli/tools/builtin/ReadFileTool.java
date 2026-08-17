package com.minicli.tools.builtin;

import com.minicli.tools.spi.Tool;
import com.minicli.tools.spi.ToolResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/** 读取指定文件内容。只读工具。 */
public final class ReadFileTool implements Tool {

    @Override
    public String name() {
        return "read_file";
    }

    @Override
    public String description() {
        return "读取指定文件的内容并返回完整文本，用于查看源码、配置或文档。";
    }

    @Override
    public JSONObject inputSchema() {
        return new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("path", new JSONObject()
                                .put("type", "string")
                                .put("description", "要读取的文件路径（相对或绝对）")))
                .put("required", new JSONArray().put("path"));
    }

    @Override
    public ToolResult invoke(JSONObject args) {
        String path = args.optString("path", "");
        if (path.isBlank()) {
            return ToolResult.failure("缺少必填参数 path");
        }
        try {
            String content = Files.readString(Path.of(path), StandardCharsets.UTF_8);
            return ToolResult.success(content);
        } catch (IOException | InvalidPathException e) {
            return ToolResult.failure("读取失败: " + e.getMessage());
        }
    }
}
