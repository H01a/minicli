package com.minicli.tools.builtin;

import com.minicli.tools.spi.Tool;
import com.minicli.tools.spi.ToolResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** 在 PATH 中查找可执行文件，返回全部匹配路径。只读工具。 */
public final class WhichTool implements Tool {

    @Override
    public String name() {
        return "which";
    }

    @Override
    public String description() {
        return "在 PATH 中查找可执行文件并返回全部匹配路径；找不到返回失败。";
    }

    @Override
    public JSONObject inputSchema() {
        return new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("command", new JSONObject()
                                .put("type", "string")
                                .put("description", "要查找的命令名")))
                .put("required", new JSONArray().put("command"));
    }

    @Override
    public ToolResult invoke(JSONObject args) {
        String command = args.optString("command", "");
        if (command.isBlank()) {
            return ToolResult.failure("缺少必填参数 command");
        }
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) {
            return ToolResult.failure("PATH 未设置");
        }
        List<String> found = new ArrayList<>();
        for (String dir : pathEnv.split(File.pathSeparator)) {
            if (dir.isBlank()) {
                continue;
            }
            Path candidate = Paths.get(dir, command);
            if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                found.add(candidate.toString());
            }
        }
        return found.isEmpty()
                ? ToolResult.failure("未找到: " + command)
                : ToolResult.success(String.join("\n", found));
    }
}
