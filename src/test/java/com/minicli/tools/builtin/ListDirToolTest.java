package com.minicli.tools.builtin;

import com.minicli.tools.spi.ToolResult;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListDirToolTest {

    @TempDir
    Path tempDir;

    @Test
    void listsEntriesWithType() throws Exception {
        Files.createDirectories(tempDir.resolve("sub"));
        Files.writeString(tempDir.resolve("a.txt"), "a");

        ToolResult result = new ListDirTool().invoke(new JSONObject().put("path", tempDir.toString()));

        assertTrue(result.isSuccess());
        assertTrue(result.output().contains("dir sub"));
        assertTrue(result.output().contains("file a.txt"));
    }

    @Test
    void failsOnMissingDir() {
        ToolResult result = new ListDirTool()
                .invoke(new JSONObject().put("path", tempDir.resolve("nope").toString()));

        assertFalse(result.isSuccess());
    }
}
