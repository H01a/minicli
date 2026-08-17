package com.minicli.tools.builtin;

import com.minicli.tools.spi.Tool;
import com.minicli.tools.spi.ToolResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/** 以树形列出目录（可选 maxDepth），默认深度 3，跳过 .git。只读工具。 */
public final class TreeTool implements Tool {

    @Override
    public String name() {
        return "tree";
    }

    @Override
    public String description() {
        return "以树形列出目录内容（maxDepth 默认 3，范围 1-10），跳过 .git。";
    }

    @Override
    public JSONObject inputSchema() {
        return new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("path", new JSONObject()
                                .put("type", "string")
                                .put("description", "要列出的目录路径（支持 ~）"))
                        .put("maxDepth", new JSONObject()
                                .put("type", "integer")
                                .put("description", "最大深度，默认 3，范围 1-10")))
                .put("required", new JSONArray());
    }

    @Override
    public ToolResult invoke(JSONObject args) {
        Path root = PathUtil.resolve(args.optString("path", "."));
        if (!Files.isDirectory(root)) {
            return ToolResult.failure("不是目录: " + root);
        }
        int maxDepth = Math.max(1, Math.min(10, args.optInt("maxDepth", 3)));
        StringBuilder sb = new StringBuilder();
        try (Stream<Path> walk = Files.walk(root, maxDepth)) {
            walk.filter(p -> !isGit(p))
                    .sorted()
                    .forEach(p -> {
                        int depth = root.relativize(p).getNameCount();
                        if (depth == 0) {
                            sb.append(p).append('\n');
                        } else {
                            sb.append("  ".repeat(depth - 1))
                                    .append(Files.isDirectory(p) ? "dir " : "file ")
                                    .append(p.getFileName())
                                    .append('\n');
                        }
                    });
        } catch (IOException e) {
            return ToolResult.failure("列出目录树失败: " + e.getMessage());
        }
        String text = sb.toString().trim();
        return ToolResult.success(text.isEmpty() ? "(空目录)" : text);
    }

    private static boolean isGit(Path p) {
        for (Path element : p) {
            if (element.getFileName() != null && element.getFileName().toString().equals(".git")) {
                return true;
            }
        }
        return false;
    }
}
