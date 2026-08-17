package com.minicli.tools.builtin;

import com.minicli.tools.spi.ToolResult;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeToolTest {

    @TempDir
    Path tempDir;

    @Test
    void respectsMaxDepth() throws IOException {
        Files.createDirectories(tempDir.resolve("a/b"));
        Files.writeString(tempDir.resolve("a/one.txt"), "x");
        Files.writeString(tempDir.resolve("a/b/two.txt"), "x");

        ToolResult result = new TreeTool().invoke(new JSONObject()
                .put("path", tempDir.toString())
                .put("maxDepth", 2));

        assertTrue(result.isSuccess());
        assertTrue(result.output().contains("one.txt"));
        assertFalse(result.output().contains("two.txt"));
    }

    @Test
    void skipsGitDir() throws IOException {
        Files.createDirectories(tempDir.resolve(".git"));
        Files.writeString(tempDir.resolve(".git/config"), "x");
        Files.writeString(tempDir.resolve("ok.txt"), "x");

        ToolResult result = new TreeTool().invoke(new JSONObject().put("path", tempDir.toString()));

        assertTrue(result.isSuccess());
        assertTrue(result.output().contains("ok.txt"));
        assertFalse(result.output().contains(".git"));
    }

    @Test
    void failsOnFile() throws IOException {
        Path file = tempDir.resolve("a.txt");
        Files.writeString(file, "x");

        ToolResult result = new TreeTool().invoke(new JSONObject().put("path", file.toString()));

        assertFalse(result.isSuccess());
        assertTrue(result.error().contains("不是目录"));
    }
}
