package com.minicli.tools.builtin;

import com.minicli.tools.spi.ToolResult;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PathInfoToolTest {

    @TempDir
    Path tempDir;

    @Test
    void reportsFileInfo() throws IOException {
        Path file = tempDir.resolve("a.txt");
        Files.writeString(file, "hello");

        ToolResult result = new PathInfoTool().invoke(new JSONObject().put("path", file.toString()));

        assertTrue(result.isSuccess());
        assertTrue(result.output().contains("type=file"));
        assertTrue(result.output().contains("size=5"));
    }

    @Test
    void reportsDirectoryType() {
        ToolResult result = new PathInfoTool().invoke(new JSONObject().put("path", tempDir.toString()));
        assertTrue(result.isSuccess());
        assertTrue(result.output().contains("type=dir"));
    }

    @Test
    void reportsMissingPath() {
        ToolResult result = new PathInfoTool().invoke(new JSONObject().put("path", tempDir.resolve("nope.txt").toString()));
        assertTrue(result.isSuccess());
        assertTrue(result.output().contains("不存在"));
    }
}
