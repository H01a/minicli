package com.minicli;

import com.minicli.config.Config;
import com.minicli.config.ConfigException;
import com.minicli.llm.DeepSeekClient;
import com.minicli.ui.Repl;

/**
 * minicli 入口：装配 Config / DeepSeekClient / JLine REPL 并启动。
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try {
            Config config = Config.load();
            new Repl(new DeepSeekClient(config)).start();
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
