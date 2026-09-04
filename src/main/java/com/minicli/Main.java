package com.minicli;

import com.minicli.agent.core.ReActAgent;
import com.minicli.config.Config;
import com.minicli.config.ConfigException;
import com.minicli.llm.DeepSeekClient;
import com.minicli.tools.builtin.EditFileTool;
import com.minicli.tools.builtin.GetCwdTool;
import com.minicli.tools.builtin.GetEnvTool;
import com.minicli.tools.builtin.GitDiffTool;
import com.minicli.tools.builtin.GitStatusTool;
import com.minicli.tools.builtin.GlobTool;
import com.minicli.tools.builtin.ListDirTool;
import com.minicli.tools.builtin.PathInfoTool;
import com.minicli.tools.builtin.ReadFileTool;
import com.minicli.tools.builtin.ReadFileRangeTool;
import com.minicli.tools.builtin.RunCommandTool;
import com.minicli.tools.builtin.SearchTextTool;
import com.minicli.tools.builtin.SystemInfoTool;
import com.minicli.tools.builtin.TreeTool;
import com.minicli.tools.builtin.WhichTool;
import com.minicli.tools.builtin.WriteFileTool;
import com.minicli.tools.builtin.web.GLMWebSearchTool;
import com.minicli.tools.spi.ToolRegistry;
import com.minicli.ui.Repl;

/**
 * minicli 入口：装配 Config / ToolRegistry / DeepSeekClient / ReActAgent / JLine REPL 并启动。
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try {
            // 到.env加载配置，包括key, model_name, url
            Config config = Config.load();
            // 将工具注册到Map<String, Tool>注册表中
            ToolRegistry registry = new ToolRegistry();
            registry.register(new ReadFileTool());
            registry.register(new ListDirTool());
            registry.register(new GlobTool());
            registry.register(new GetCwdTool());
            registry.register(new PathInfoTool());
            registry.register(new TreeTool());
            registry.register(new SearchTextTool());
            registry.register(new ReadFileRangeTool());
            registry.register(new WriteFileTool());
            registry.register(new EditFileTool());
            registry.register(new RunCommandTool());
            registry.register(new GetEnvTool());
            registry.register(new SystemInfoTool());
            registry.register(new GitStatusTool());
            registry.register(new GitDiffTool());
            registry.register(new WhichTool());
            if (!config.glmApiKey().isBlank()) {
                registry.register(new GLMWebSearchTool(config.glmApiKey()));
            } else {
                System.err.println("[main] 未配置 GLM_API_KEY，跳过 glm_web_search 工具注册");
            }
            ReActAgent agent = new ReActAgent(new DeepSeekClient(config), registry,
                    config.maxSteps(), config.maxConcurrency(), config.maxObservationChars());
            new Repl(agent).start();
        } catch (ConfigException e) {
            System.err.println("配置错误: " + e.getMessage());
            System.exit(1);
        }
    }

    /** 保留：环境自检用。 */
    public static String greeting() {
        return "Hello from minicli (Java %s)".formatted(System.getProperty("java.version"));
    }
}
