package com.minicli.tools.builtin;

import com.minicli.tools.spi.Tool;
import com.minicli.tools.spi.ToolResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.stream.Stream;

/** 按 glob 模式查找文件，返回相对 baseDir 的路径列表。只读工具。 */
public final class GlobTool implements Tool {

    @Override
    public String name() {
        return "glob";
    }

    @Override
    public String description() {
        return "按 glob 模式查找文件并返回路径列表，如 pattern=\"src/**/*.java\"。";
    }

    @Override
    public JSONObject inputSchema() {
        return new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("pattern", new JSONObject()
                                .put("type", "string")
                                .put("description", "glob 模式，如 src/**/*.java"))
                        .put("baseDir", new JSONObject()
                                .put("type", "string")
                                .put("description", "搜索根目录，缺省为当前目录")))
                .put("required", new JSONArray().put("pattern"));
    }

    @Override
    public ToolResult invoke(JSONObject args) {
        String pattern = args.optString("pattern", "");
        if (pattern.isBlank()) {
            return ToolResult.failure("缺少必填参数 pattern");
        }
        String baseDir = args.optString("baseDir", ".");
        Path root;
        try {
            root = Path.of(baseDir).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            return ToolResult.failure("无效路径: " + e.getMessage());
        }
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        try (Stream<Path> walk = Files.walk(root)) {
            List<String> matches = walk
                    .filter(Files::isRegularFile)
                    .map(p -> root.relativize(p).toString().replace('\\', '/'))
                    .filter(rel -> matcher.matches(Path.of(rel)))
                    .sorted()
                    .toList();
            return ToolResult.success(matches.isEmpty() ? "(无匹配)" : String.join("\n", matches));
        } catch (IOException e) {
            return ToolResult.failure("glob 查找失败: " + e.getMessage());
        }
    }
}
