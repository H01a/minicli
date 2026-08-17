package com.minicli.tools.builtin;

import com.minicli.tools.spi.Tool;
import com.minicli.tools.spi.ToolResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** 写入文件内容（可自动创建父目录）；拒绝 .env* 与 .git 路径。 */
public final class WriteFileTool implements Tool {

    @Override
    public String name() {
        return "write_file";
    }

    @Override
    public String description() {
        return "写入文件内容（可自动创建父目录）；出于安全拒绝 .env* 与 .git 路径。";
    }

    @Override
    public JSONObject inputSchema() {
        return new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("path", new JSONObject()
                                .put("type", "string")
                                .put("description", "要写入的文件路径（支持 ~）"))
                        .put("content", new JSONObject()
                                .put("type", "string")
                                .put("description", "完整文件内容"))
                        .put("createDirs", new JSONObject()
                                .put("type", "boolean")
                                .put("description", "是否自动创建父目录，默认 true")))
                .put("required", new JSONArray().put("path").put("content"));
    }

    @Override
    public ToolResult invoke(JSONObject args) {
        String path = args.optString("path", "");
        if (path.isBlank()) {
            return ToolResult.failure("缺少必填参数 path");
        }
        if (!args.has("content")) {
            return ToolResult.failure("缺少必填参数 content");
        }
        String content = args.optString("content");
        Path target = PathUtil.resolve(path);
        if (PathUtil.isSensitiveWritePath(target)) {
            return ToolResult.failure("拒绝写入敏感路径: " + target);
        }
        boolean createDirs = args.optBoolean("createDirs", true);
        try {
            if (createDirs && target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            Files.write(target, bytes);
            return ToolResult.success("已写入 " + bytes.length + " 字节: " + target);
        } catch (IOException e) {
            return ToolResult.failure("写入失败: " + e.getMessage());
        }
    }
}
