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

class WriteFileToolTest {

    @TempDir
    Path tempDir;

    @Test
    void writesFileWithDirs() throws IOException {
        Path target = tempDir.resolve("nested/a.txt");

        ToolResult result = new WriteFileTool().invoke(new JSONObject()
                .put("path", target.toString())
                .put("content", "hello"));

        assertTrue(result.isSuccess());
        assertTrue(result.output().contains("已写入 5 字节"));
        assertEquals("hello", Files.readString(target));
    }

    @Test
    void rejectsEnvPath() {
        ToolResult result = new WriteFileTool().invoke(new JSONObject()
                .put("path", tempDir.resolve(".env").toString())
                .put("content", "x"));

        assertFalse(result.isSuccess());
        assertTrue(result.error().contains("敏感路径"));
    }

    @Test
    void rejectsGitPath() {
        ToolResult result = new WriteFileTool().invoke(new JSONObject()
                .put("path", tempDir.resolve("a/.git/config").toString())
                .put("content", "x"));

        assertFalse(result.isSuccess());
        assertTrue(result.error().contains("敏感路径"));
    }
}
