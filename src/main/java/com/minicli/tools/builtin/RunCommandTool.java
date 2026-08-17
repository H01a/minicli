package com.minicli.tools.builtin;

import com.minicli.tools.spi.Tool;
import com.minicli.tools.spi.ToolResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** 在指定目录执行 shell 命令（默认当前目录），超时默认 30s，输出截断 8000 字符。 */
public final class RunCommandTool implements Tool {

    public static final int DEFAULT_TIMEOUT_SECONDS = 30;
    public static final int MAX_TIMEOUT_SECONDS = 120;

    @Override
    public String name() {
        return "run_command";
    }

    @Override
    public String description() {
        return "在指定目录执行 shell 命令并返回合并输出（exit 码 + stdout/stderr），默认超时 30 秒、输出截断 8000 字符。";
    }

    @Override
    public JSONObject inputSchema() {
        return new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("command", new JSONObject()
                                .put("type", "string")
                                .put("description", "要执行的 shell 命令"))
                        .put("cwd", new JSONObject()
                                .put("type", "string")
                                .put("description", "执行目录（支持 ~），默认当前目录"))
                        .put("timeoutSeconds", new JSONObject()
                                .put("type", "integer")
                                .put("description", "超时秒数，默认 30，最大 120")))
                .put("required", new JSONArray().put("command"));
    }

    @Override
    public ToolResult invoke(JSONObject args) {
        String command = args.optString("command", "");
        if (command.isBlank()) {
            return ToolResult.failure("缺少必填参数 command");
        }
        int timeout = args.optInt("timeoutSeconds", DEFAULT_TIMEOUT_SECONDS);
        if (timeout <= 0 || timeout > MAX_TIMEOUT_SECONDS) {
            return ToolResult.failure("timeoutSeconds 需在 1.." + MAX_TIMEOUT_SECONDS);
        }
        Path cwd = PathUtil.resolve(args.optString("cwd", "."));
        if (!Files.isDirectory(cwd)) {
            return ToolResult.failure("cwd 不是目录: " + cwd);
        }
        List<String> commandLine = new ArrayList<>();
        commandLine.add(shell());
        commandLine.add(shellFlag());
        commandLine.add(command);
        return CommandRunner.run(cwd, commandLine, timeout, false, CommandRunner.DEFAULT_MAX_OUTPUT_CHARS);
    }

    private static String shell() {
        return isWindows() ? "cmd" : "sh";
    }

    private static String shellFlag() {
        return isWindows() ? "/c" : "-c";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
