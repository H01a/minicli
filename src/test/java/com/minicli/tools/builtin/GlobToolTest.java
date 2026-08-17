package com.minicli.tools.builtin;

import com.minicli.tools.spi.ToolResult;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobToolTest {

    @TempDir
    Path tempDir;

    @Test
    void matchesJavaFilesRecursively() throws Exception {
        Files.createDirectories(tempDir.resolve("src/sub"));
        Files.writeString(tempDir.resolve("src/sub/A.java"), "class A {}");
        Files.writeString(tempDir.resolve("src/B.java"), "class B {}");
        Files.writeString(tempDir.resolve("src/readme.txt"), "x");

        ToolResult result = new GlobTool().invoke(new JSONObject()
                .put("pattern", "**/*.java")
                .put("baseDir", tempDir.toString()));

        assertTrue(result.isSuccess());
        assertTrue(result.output().contains("src/sub/A.java"));
        assertTrue(result.output().contains("src/B.java"));
        assertFalse(result.output().contains("readme.txt"));
    }

    @Test
    void failsOnMissingPattern() {
        ToolResult result = new GlobTool().invoke(new JSONObject());

        assertFalse(result.isSuccess());
        assertTrue(result.error().contains("pattern"));
    }
}
