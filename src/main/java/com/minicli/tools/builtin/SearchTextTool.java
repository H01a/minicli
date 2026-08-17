package com.minicli.tools.builtin;

import com.minicli.tools.spi.Tool;
import com.minicli.tools.spi.ToolResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

/** 在目录内按文本/正则搜索文件内容，返回 文件:行号:内容。只读工具。 */
public final class SearchTextTool implements Tool {

    private static final int DEFAULT_MAX_RESULTS = 50;
    private static final Set<String> SKIP_DIRS = Set.of(".git", "target", "node_modules", ".idea");

    @Override
    public String name() {
        return "search_text";
    }

    @Override
    public String description() {
        return "按文本/正则搜索文件内容并返回 文件:行号:内容；默认跳过 .git/target/node_modules/.idea。";
    }

    @Override
    public JSONObject inputSchema() {
        return new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("pattern", new JSONObject()
                                .put("type", "string")
                                .put("description", "搜索的文本或正则表达式"))
                        .put("path", new JSONObject()
                                .put("type", "string")
                                .put("description", "搜索根目录（支持 ~），默认当前目录"))
                        .put("caseSensitive", new JSONObject()
                                .put("type", "boolean")
                                .put("description", "是否区分大小写，默认否"))
                        .put("maxResults", new JSONObject()
                                .put("type", "integer")
                                .put("description", "最大结果数，默认 50")))
                .put("required", new JSONArray().put("pattern"));
    }

    @Override
    public ToolResult invoke(JSONObject args) {
        String patternText = args.optString("pattern", "");
        if (patternText.isBlank()) {
            return ToolResult.failure("缺少必填参数 pattern");
        }
        int flags = args.optBoolean("caseSensitive", false) ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        Pattern pattern;
        try {
            pattern = Pattern.compile(patternText, flags);
        } catch (PatternSyntaxException e) {
            return ToolResult.failure("无效的正则表达式: " + e.getMessage());
        }
        int maxResults = args.optInt("maxResults", DEFAULT_MAX_RESULTS);
        if (maxResults <= 0) {
            return ToolResult.failure("maxResults 必须为正整数");
        }
        Path root = PathUtil.resolve(args.optString("path", "."));
        List<String> hits = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> !isSkipped(p))
                    .sorted()
                    .forEach(p -> {
                        if (hits.size() >= maxResults) {
                            return;
                        }
                        try {
                            List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
                            for (int i = 0; i < lines.size() && hits.size() < maxResults; i++) {
                                if (pattern.matcher(lines.get(i)).find()) {
                                    hits.add(root.relativize(p) + ":" + (i + 1) + ": " + lines.get(i).trim());
                                }
                            }
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException e) {
            return ToolResult.failure("搜索失败: " + e.getMessage());
        }
        if (hits.isEmpty()) {
            return ToolResult.success("(无匹配)");
        }
        return ToolResult.success(String.join("\n", hits)
                + (hits.size() >= maxResults ? "\n…(已达最大结果数)" : ""));
    }

    private static boolean isSkipped(Path p) {
        for (Path element : p) {
            String name = element.getFileName() == null ? "" : element.getFileName().toString();
            if (SKIP_DIRS.contains(name)) {
                return true;
            }
        }
        return false;
    }
}
