package com.minicli.tools.builtin;

import com.minicli.tools.spi.Tool;
import com.minicli.tools.spi.ToolResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** 在指定目录执行 git status --short；非 git 仓库返回失败。只读工具。 */
public final class GitStatusTool implements Tool {

    public static final int TIMEOUT_SECONDS = 15;

    @Override
    public String name() {
        return "git_status";
    }

    @Override
    public String description() {
        return "在指定目录（默认当前目录）执行 git status --short，返回工作区状态；非 git 仓库返回失败。";
    }

    @Override
    public JSONObject inputSchema() {
        return new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("path", new JSONObject()
                                .put("type", "string")
                                .put("description", "仓库目录（支持 ~），默认当前目录")))
                .put("required", new JSONArray());
    }

    @Override
    public ToolResult invoke(JSONObject args) {
        Path cwd = PathUtil.resolve(args.optString("path", "."));
        if (!Files.isDirectory(cwd)) {
            return ToolResult.failure("不是目录: " + cwd);
        }
        List<String> cmd = new ArrayList<>();
        cmd.add("git");
        cmd.add("-C");
        cmd.add(cwd.toString());
        cmd.add("status");
        cmd.add("--short");
        return CommandRunner.run(cwd, cmd, TIMEOUT_SECONDS, true, 4000);
    }
}
