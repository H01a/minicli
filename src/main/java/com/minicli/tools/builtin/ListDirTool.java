package com.minicli.tools.builtin;

import com.minicli.tools.spi.Tool;
import com.minicli.tools.spi.ToolResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** 列出目录条目（名称 + 类型 file/dir）。只读工具。 */
public final class ListDirTool implements Tool {

    @Override
    public String name() {
        return "list_dir";
    }

    @Override
    public String description() {
        return "列出指定目录下的条目（每行：类型 + 名称），默认当前目录。";
    }

    @Override
    public JSONObject inputSchema() {
        return new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("path", new JSONObject()
                                .put("type", "string")
                                .put("description", "要列出的目录路径，缺省为当前目录")))
                .put("required", new JSONArray());
    }

    @Override
    public ToolResult invoke(JSONObject args) {
        String path = args.optString("path", ".");
        try (Stream<Path> stream = Files.list(Path.of(path))) {
            String output = stream
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .map(p -> (Files.isDirectory(p) ? "dir " : "file ") + p.getFileName())
                    .collect(Collectors.joining("\n"));
            return ToolResult.success(output.isEmpty() ? "(空目录)" : output);
        } catch (IOException e) {
            return ToolResult.failure("列出目录失败: " + e.getMessage());
        }
    }
}
