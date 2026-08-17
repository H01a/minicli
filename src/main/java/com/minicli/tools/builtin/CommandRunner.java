package com.minicli.tools.builtin;

import com.minicli.tools.spi.ToolResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** 内置工具的命令执行辅助：合并 stderr/stdout、超时、输出截断。 */
final class CommandRunner {

    static final int DEFAULT_MAX_OUTPUT_CHARS = 8000;

    private CommandRunner() {
    }

    /**
     * 执行命令并返回合并输出。nonZeroIsFailure=true 时非零退出码转 FAILURE（git 等）；
     * false 时非零退出码也返回 SUCCESS（run_command 让模型自己判断）。
     */
    static ToolResult run(Path cwd, List<String> command, int timeoutSeconds,
                          boolean nonZeroIsFailure, int maxOutputChars) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(cwd.toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            Thread reader = Thread.startVirtualThread(() -> {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(
                        process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (output.length() <= maxOutputChars) {
                            output.append(line).append('\n');
                        }
                    }
                } catch (IOException ignored) {
                }
            });
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return ToolResult.failure("命令超时（" + timeoutSeconds + "s）");
            }
            reader.join();
            int exit = process.exitValue();
            String text = PathUtil.abbreviate(output.toString().trim(), maxOutputChars);
            if (exit != 0 && nonZeroIsFailure) {
                return ToolResult.failure("exit=" + exit + "\n" + (text.isEmpty() ? "(无输出)" : text));
            }
            return ToolResult.success("exit=" + exit + "\n" + (text.isEmpty() ? "(无输出)" : text));
        } catch (IOException e) {
            return ToolResult.failure("执行命令失败: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ToolResult.failure("执行被中断");
        }
    }
}
