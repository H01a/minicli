package com.minicli.tools.builtin;

import com.minicli.tools.spi.Tool;
import com.minicli.tools.spi.ToolResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** 显示 git diff（statOnly=true 时只显示统计），可指定文件路径；非 git 仓库返回失败。只读工具。 */
public final class GitDiffTool implements Tool {

    public static final int TIMEOUT_SECONDS = 15;

    @Override
    public String name() {
        return "git_diff";
    }

    @Override
    public String description() {
        return "显示 git diff（statOnly=true 时只显示统计），可指定文件路径（支持 ~）；非 git 仓库返回失败。";
    }

    @Override
    public JSONObject inputSchema() {
        return new JSONObject()
                .put("type", "object")
                .put("properties", new JSONObject()
                        .put("path", new JSONObject()
                                .put("type", "string")
                                .put("description", "要查看 diff 的文件或目录（支持 ~），默认当前目录"))
                        .put("statOnly", new JSONObject()
                                .put("type", "boolean")
                                .put("description", "只显示 diff 统计，默认 false")))
                .put("required", new JSONArray());
    }

    @Override
    public ToolResult invoke(JSONObject args) {
        Path target = PathUtil.resolve(args.optString("path", "."));
        Path cwd = Files.isDirectory(target) ? target
                : (target.getParent() != null ? target.getParent() : PathUtil.resolve("."));
        if (!Files.isDirectory(cwd)) {
            return ToolResult.failure("不是目录: " + cwd);
        }
        boolean statOnly = args.optBoolean("statOnly", false);
        List<String> cmd = new ArrayList<>();
        cmd.add("git");
        cmd.add("-C");
        cmd.add(cwd.toString());
        cmd.add("diff");
        if (statOnly) {
            cmd.add("--stat");
        } else {
            cmd.add("--no-color");
        }
        cmd.add("--");
        cmd.add(target.toString());
        return CommandRunner.run(cwd, cmd, TIMEOUT_SECONDS, true, 4000);
    }
}
