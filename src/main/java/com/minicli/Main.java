package com.minicli;

/**
 * minicli 入口（M0 骨架阶段：helloworld 验证）。
 * 后续里程碑会在此处装配 config / JLine REPL / Agent 循环。
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        System.out.println(greeting());
    }

    /**
     * 返回带 JVM 版本号的问候语，用于验证运行环境。
     */
    public static String greeting() {
        return "Hello from minicli (Java %s)".formatted(System.getProperty("java.version"));
    }
}
