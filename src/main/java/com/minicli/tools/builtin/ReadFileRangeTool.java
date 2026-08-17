package com.minicli.tools.builtin;

import com.minicli.tools.spi.Tool;
import com.minicli.tools.spi.ToolResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** 按行范围读取文件（startLine 默认 1，endLine 缺省到末尾），返回带行号内容。只读工具。 */
public final class ReadFileRangeTool implements Tool {

    @Override
    public String name() {
        return "read_file_range";
    }

    @Override
    public String description() {
        return "按行范围读取文件并返回带行号内容（startLine 默认 1，endLine 缺省到末尾），适合大文件。";
    }

    @Override
    public JSONObject inputSchema() {
        return new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("path", new JSONObject()
                                .put("type", "string")
                                .put("description", "要读取的文件路径（支持 ~）"))
                        .put("startLine", new JSONObject()
                                .put("type", "integer")
                                .put("description", "起始行，默认 1"))
                        .put("endLine", new JSONObject()
                                .put("type", "integer")
                                .put("description", "结束行，缺省到末尾")))
                .put("required", new JSONArray().put("path"));
    }

    @Override
    public ToolResult invoke(JSONObject args) {
        String path = args.optString("path", "");
        if (path.isBlank()) {
            return ToolResult.failure("缺少必填参数 path");
        }
        Path target = PathUtil.resolve(path);
        if (!Files.isRegularFile(target)) {
            return ToolResult.failure("不是文件: " + target);
        }
        int startLine = Math.max(1, args.optInt("startLine", 1));
        int endLine = args.optInt("endLine", -1);
        try {
            List<String> lines = Files.readAllLines(target, StandardCharsets.UTF_8);
            int from = Math.min(startLine, lines.size() + 1);
            int to = endLine < 0 ? lines.size() : Math.min(endLine, lines.size());
            if (from > to) {
                return ToolResult.success("(空范围)");
            }
            StringBuilder sb = new StringBuilder();
            for (int i = from; i <= to; i++) {
                sb.append(i).append(": ").append(lines.get(i - 1)).append('\n');
            }
            return ToolResult.success(sb.toString().trim());
        } catch (IOException e) {
            return ToolResult.failure("读取失败: " + e.getMessage());
        }
    }
}
