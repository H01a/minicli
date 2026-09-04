package com.minicli.mcp.transport;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * stdio 传输：用 ProcessBuilder 启动 MCP server 子进程，
 * stdout 每行一条 JSON-RPC（newline-delimited JSON），stderr 仅转发为日志。
 */
public final class StdioTransport implements Transport {

    private final List<String> commandLine;
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    private Process process;
    private BufferedWriter stdin;
    private Thread stdoutThread;
    private Thread stderrThread;

    public StdioTransport(List<String> commandLine) {
        if (commandLine == null || commandLine.isEmpty() || commandLine.get(0).isBlank()) {
            throw new IllegalArgumentException("MCP server 启动命令不能为空");
        }
        this.commandLine = List.copyOf(commandLine);
    }

    @Override
    public void start(MessageListener listener) throws IOException {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("传输已启动，不能重复 start");
        }
        ProcessBuilder builder = new ProcessBuilder(commandLine);
        try {
            process = builder.start();
        } catch (IOException e) {
            started.set(false);
            throw new IOException("启动 MCP server 失败: " + String.join(" ", commandLine)
                    + "（" + e.getMessage() + "）", e);
        }
        stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        stdoutThread = new Thread(() -> pumpStdout(listener), "mcp-stdout-" + process.pid());
        stdoutThread.setDaemon(true);
        stdoutThread.start();
        stderrThread = new Thread(this::pumpStderr, "mcp-stderr-" + process.pid());
        stderrThread.setDaemon(true);
        stderrThread.start();
    }

    private void pumpStdout(MessageListener listener) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                try {
                    listener.onMessage(new JSONObject(line.strip()));
                } catch (JSONException e) {
                    System.err.println("[mcp] 收到非法 JSON（忽略）: " + e.getMessage());
                } catch (RuntimeException e) {
                    System.err.println("[mcp] 处理消息异常（忽略，继续读）: " + e.getMessage());
                }
            }
            listener.onClosed(null);
        } catch (IOException e) {
            if (!closed.get()) {
                listener.onClosed(e);
            }
        }
    }

    private void pumpStderr() {
        String label = commandLine.get(0);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.err.println("[mcp:" + label + "] " + line);
            }
        } catch (IOException ignored) {
            // 关闭时的正常结束
        }
    }

    @Override
    public synchronized void send(JSONObject message) throws IOException {
        if (process == null || closed.get() || !process.isAlive()) {
            throw new IOException("MCP server 进程已退出，无法发送消息");
        }
        stdin.write(message.toString());
        stdin.newLine();
        stdin.flush();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (stdin != null) {
            try {
                stdin.close();
            } catch (IOException ignored) {
                // 进程已退出等情况
            }
        }
        if (process != null && process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(3, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
    }
}
