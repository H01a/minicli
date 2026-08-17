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

class SearchTextToolTest {

    @TempDir
    Path tempDir;

    @Test
    void findsMatchesWithLineNumbers() throws IOException {
        Files.writeString(tempDir.resolve("a.java"), "hello world\nfoo bar\nHELLO again\n");

        ToolResult result = new SearchTextTool().invoke(new JSONObject()
                .put("path", tempDir.toString())
                .put("pattern", "hello"));

        assertTrue(result.isSuccess());
        assertTrue(result.output().contains("a.java:1"));
        assertTrue(result.output().contains("a.java:3"));
    }

    @Test
    void respectsCaseSensitive() throws IOException {
        Files.writeString(tempDir.resolve("a.java"), "hello world\nHELLO again\n");

        ToolResult result = new SearchTextTool().invoke(new JSONObject()
                .put("path", tempDir.toString())
                .put("pattern", "HELLO")
                .put("caseSensitive", true));

        assertTrue(result.isSuccess());
        assertTrue(result.output().contains("a.java:2"));
        assertFalse(result.output().contains("a.java:1"));
    }

    @Test
    void reportsNoMatch() throws IOException {
        Files.writeString(tempDir.resolve("a.txt"), "nothing here\n");

        ToolResult result = new SearchTextTool().invoke(new JSONObject()
                .put("path", tempDir.toString())
                .put("pattern", "zzz"));

        assertTrue(result.isSuccess());
        assertTrue(result.output().contains("无匹配"));
    }

    @Test
    void rejectsInvalidRegex() {
        ToolResult result = new SearchTextTool().invoke(new JSONObject()
                .put("path", tempDir.toString())
                .put("pattern", "["));

        assertFalse(result.isSuccess());
        assertTrue(result.error().contains("无效的正则"));
    }
}
