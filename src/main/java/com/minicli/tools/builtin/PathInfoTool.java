package com.minicli.tools.builtin;

import com.minicli.tools.spi.Tool;
import com.minicli.tools.spi.ToolResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/** 返回路径信息：是否存在、类型、大小、修改时间、绝对路径。只读工具。 */
public final class PathInfoTool implements Tool {

    @Override
    public String name() {
        return "path_info";
    }

    @Override
    public String description() {
        return "返回指定路径的信息（存在性/类型/大小/修改时间/绝对路径），默认当前目录。";
    }

    @Override
    public JSONObject inputSchema() {
        return new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("path", new JSONObject()
                                .put("type", "string")
                                .put("description", "要查询的路径（相对或绝对，支持 ~）")))
                .put("required", new JSONArray());
    }

    @Override
    public ToolResult invoke(JSONObject args) {
        Path p = PathUtil.resolve(args.optString("path", "."));
        if (!Files.exists(p)) {
            return ToolResult.success("不存在: " + p);
        }
        try {
            BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
            String type = Files.isDirectory(p) ? "dir" : (Files.isSymbolicLink(p) ? "symlink" : "file");
            String size = attrs.isDirectory() ? "-" : Long.toString(attrs.size());
            return ToolResult.success("""
                    path=%s
                    type=%s
                    size=%s
                    lastModified=%s
                    """.formatted(p, type, size, attrs.lastModifiedTime().toInstant()));
        } catch (IOException e) {
            return ToolResult.failure("读取路径信息失败: " + e.getMessage());
        }
    }
}
