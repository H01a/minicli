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

class GitStatusToolTest {

    @TempDir
    Path tempDir;

    @Test
    void reportsShortStatusInRepo() throws Exception {
        initRepo(tempDir);
        Files.writeString(tempDir.resolve("new.txt"), "x");

        ToolResult result = new GitStatusTool().invoke(new JSONObject().put("path", tempDir.toString()));

        assertTrue(result.isSuccess());
        assertTrue(result.output().contains("new.txt"));
    }

    @Test
    void failsOutsideRepo() {
        ToolResult result = new GitStatusTool().invoke(new JSONObject().put("path", tempDir.toString()));
        assertFalse(result.isSuccess());
    }

    private static void initRepo(Path dir) throws Exception {
        Process process = new ProcessBuilder("git", "init", "-q", dir.toString()).start();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("git init 超时");
        }
        assertEquals(0, process.exitValue());
    }
}
