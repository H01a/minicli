package com.minicli.tools.builtin;

import com.minicli.tools.spi.ToolResult;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitDiffToolTest {

    @TempDir
    Path tempDir;

    @Test
    void showsDiffAfterModification() throws Exception {
        initRepo(tempDir);
        Path file = tempDir.resolve("a.txt");
        Files.writeString(file, "one");
        run("add", "a.txt");
        Files.writeString(file, "two");

        ToolResult result = new GitDiffTool().invoke(new JSONObject().put("path", file.toString()));

        assertTrue(result.isSuccess());
        assertTrue(result.output().contains("a.txt"));
    }

    @Test
    void showsStatOnly() throws Exception {
        initRepo(tempDir);
        Path file = tempDir.resolve("a.txt");
        Files.writeString(file, "one");
        run("add", "a.txt");
        Files.writeString(file, "two");

        ToolResult result = new GitDiffTool().invoke(new JSONObject()
                .put("path", file.toString())
                .put("statOnly", true));

        assertTrue(result.isSuccess());
        assertTrue(result.output().contains("a.txt"));
    }

    @Test
    void failsOutsideRepo() {
        ToolResult result = new GitDiffTool().invoke(new JSONObject().put("path", tempDir.toString()));
        assertFalse(result.isSuccess());
    }

    private void initRepo(Path dir) throws Exception {
        Process process = new ProcessBuilder("git", "init", "-q", dir.toString()).start();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("git init 超时");
        }
        assertEquals(0, process.exitValue());
    }

    private void run(String... args) throws Exception {
        java.util.List<String> cmd = new java.util.ArrayList<>();
        cmd.add("git");
        cmd.add("-C");
        cmd.add(tempDir.toString());
        cmd.addAll(java.util.List.of(args));
        Process process = new ProcessBuilder(cmd).start();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("git 命令超时");
        }
        assertEquals(0, process.exitValue());
    }
}
