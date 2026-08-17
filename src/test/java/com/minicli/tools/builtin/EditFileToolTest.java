package com.minicli.tools.builtin;

import com.minicli.tools.spi.ToolResult;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditFileToolTest {

    @TempDir
    Path tempDir;

    @Test
    void replacesAllOccurrences() throws IOException {
        Path file = tempDir.resolve("a.txt");
        Files.writeString(file, "aa bb aa");

        ToolResult result = new EditFileTool().invoke(new JSONObject()
                .put("path", file.toString())
                .put("oldText", "aa")
                .put("newText", "x"));

        assertTrue(result.isSuccess());
        assertTrue(result.output().contains("已替换 2 处"));
        assertEquals("x bb x", Files.readString(file));
    }

    @Test
    void reportsNotFoundAndKeepsFile() throws IOException {
        Path file = tempDir.resolve("a.txt");
        Files.writeString(file, "keep me");

        ToolResult result = new EditFileTool().invoke(new JSONObject()
                .put("path", file.toString())
                .put("oldText", "zzz")
                .put("newText", "x"));

        assertFalse(result.isSuccess());
        assertTrue(result.error().contains("未找到"));
        assertEquals("keep me", Files.readString(file));
    }

    @Test
    void rejectsEnvPath() {
        ToolResult result = new EditFileTool().invoke(new JSONObject()
                .put("path", tempDir.resolve(".env").toString())
                .put("oldText", "a")
                .put("newText", "b"));

        assertFalse(result.isSuccess());
        assertTrue(result.error().contains("敏感路径"));
    }
}
