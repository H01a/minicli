package com.minicli;

import com.minicli.agent.core.ReActAgent;
import com.minicli.config.Config;
import com.minicli.config.ConfigException;
import com.minicli.llm.DeepSeekClient;
import com.minicli.tools.builtin.GlobTool;
import com.minicli.tools.builtin.ListDirTool;
import com.minicli.tools.builtin.ReadFileTool;
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
