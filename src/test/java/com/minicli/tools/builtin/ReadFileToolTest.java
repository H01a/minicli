package com.minicli.tools.builtin;

import com.minicli.tools.spi.ToolResult;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadFileToolTest {

    @TempDir
    Path tempDir;

    @Test
    void readsExistingFile() throws Exception {
        Path file = tempDir.resolve("hello.txt");
        Files.writeString(file, "hello minicli");

        ToolResult result = new ReadFileTool().invoke(new JSONObject().put("path", file.toString()));

        assertTrue(result.isSuccess());
        assertEquals("hello minicli", result.output());
    }

    @Test
    void failsOnMissingFile() {
        ToolResult result = new ReadFileTool()
                .invoke(new JSONObject().put("path", tempDir.resolve("nope.txt").toString()));

        assertFalse(result.isSuccess());
        assertTrue(result.error().contains("读取失败"));
    }

    @Test
    void failsOnMissingPathArg() {
        ToolResult result = new ReadFileTool().invoke(new JSONObject());

        assertFalse(result.isSuccess());
        assertTrue(result.error().contains("path"));
    }
}
