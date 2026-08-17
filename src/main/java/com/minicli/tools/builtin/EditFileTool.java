package com.minicli.tools.builtin;

import com.minicli.tools.spi.Tool;
import com.minicli.tools.spi.ToolResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** 在文件中精确替换 oldText 为 newText（全部出现），返回替换摘要；拒绝 .env* 与 .git 路径。 */
public final class EditFileTool implements Tool {

    @Override
    public String name() {
        return "edit_file";
    }

    @Override
    public String description() {
        return "在文件中精确替换 oldText 为 newText（全部出现），返回替换摘要；出于安全拒绝 .env* 与 .git 路径。";
    }

    @Override
    public JSONObject inputSchema() {
        return new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("path", new JSONObject()
                                .put("type", "string")
                                .put("description", "要编辑的文件路径（支持 ~）"))
                        .put("oldText", new JSONObject()
                                .put("type", "string")
                                .put("description", "要替换的原文（必须精确匹配）"))
                        .put("newText", new JSONObject()
                                .put("type", "string")
                                .put("description", "替换后的文本")))
                .put("required", new JSONArray().put("path").put("oldText").put("newText"));
    }

    @Override
    public ToolResult invoke(JSONObject args) {
        String path = args.optString("path", "");
        String oldText = args.optString("oldText", "");
        String newText = args.optString("newText", "");
        if (path.isBlank()) {
            return ToolResult.failure("缺少必填参数 path");
        }
        if (oldText.isBlank()) {
            return ToolResult.failure("oldText 不能为空");
        }
        Path target = PathUtil.resolve(path);
        if (PathUtil.isSensitiveWritePath(target)) {
            return ToolResult.failure("拒绝编辑敏感路径: " + target);
        }
        if (!Files.isRegularFile(target)) {
            return ToolResult.failure("不是文件: " + target);
        }
        try {
            String original = Files.readString(target, StandardCharsets.UTF_8);
            int count = countOccurrences(original, oldText);
            if (count == 0) {
                return ToolResult.failure("未找到要替换的文本");
            }
            Files.writeString(target, original.replace(oldText, newText), StandardCharsets.UTF_8);
            return ToolResult.success("已替换 " + count + " 处: " + target + "\n"
                    + "- " + PathUtil.abbreviate(oldText, 120) + "\n"
                    + "+ " + PathUtil.abbreviate(newText, 120));
        } catch (IOException e) {
            return ToolResult.failure("编辑失败: " + e.getMessage());
        }
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
