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

class ReadFileRangeToolTest {

    @TempDir
    Path tempDir;

    @Test
    void readsLineRange() throws IOException {
        Path file = tempDir.resolve("a.txt");
        Files.writeString(file, "line1\nline2\nline3\nline4\nline5\n");

        ToolResult result = new ReadFileRangeTool().invoke(new JSONObject()
                .put("path", file.toString())
                .put("startLine", 2)
                .put("endLine", 4));

        assertTrue(result.isSuccess());
        assertTrue(result.output().contains("2: line2"));
        assertTrue(result.output().contains("4: line4"));
        assertFalse(result.output().contains("1: line1"));
    }

    @Test
    void readsToEndByDefault() throws IOException {
        Path file = tempDir.resolve("a.txt");
        Files.writeString(file, "line1\nline2\n");

        ToolResult result = new ReadFileRangeTool().invoke(new JSONObject()
                .put("path", file.toString())
                .put("startLine", 2));

        assertTrue(result.isSuccess());
        assertTrue(result.output().contains("2: line2"));
        assertFalse(result.output().contains("1: line1"));
    }

    @Test
    void failsOnMissingFile() {
        ToolResult result = new ReadFileRangeTool().invoke(new JSONObject()
                .put("path", tempDir.resolve("nope.txt").toString()));

        assertFalse(result.isSuccess());
        assertTrue(result.error().contains("不是文件"));
    }
}
