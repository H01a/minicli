package com.minicli.mcp.registry;

import com.minicli.mcp.protocol.McpException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpServerLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void missingFileReturnsEmpty() throws IOException {
        assertTrue(McpServerLoader.load(tempDir.resolve("missing.json")).isEmpty());
    }

    @Test
    void parsesServersAndCommandLine() throws IOException {
        Path file = tempDir.resolve("mcp.json");
        Files.writeString(file, """
                {
                  "servers": [
                    {"name": "everything", "command": "npx", "args": ["-y", "pkg"]},
                    {"name": "bare", "command": "echo"}
                  ]
                }
                """);

        List<McpServerConfig> servers = McpServerLoader.load(file);

        assertEquals(2, servers.size());
        assertEquals("everything", servers.get(0).name());
        assertEquals(List.of("npx", "-y", "pkg"), servers.get(0).commandLine());
        assertEquals(List.of("echo"), servers.get(1).commandLine());
    }

    @Test
    void duplicateServerNameRejected() throws IOException {
        Path file = tempDir.resolve("mcp.json");
        Files.writeString(file, """
                {
                  "servers": [
                    {"name": "same", "command": "a"},
                    {"name": "same", "command": "b"}
                  ]
                }
                """);

        McpException ex = assertThrows(McpException.class, () -> McpServerLoader.load(file));

        assertTrue(ex.getMessage().contains("重复"));
    }

    @Test
    void invalidJsonRejected() throws IOException {
        Path file = tempDir.resolve("mcp.json");
        Files.writeString(file, "not-json");

        McpException ex = assertThrows(McpException.class, () -> McpServerLoader.load(file));

        assertTrue(ex.getMessage().contains("不是合法 JSON"));
    }

    @Test
    void missingServersArrayRejected() throws IOException {
        Path file = tempDir.resolve("mcp.json");
        Files.writeString(file, "{\"other\": []}");

        McpException ex = assertThrows(McpException.class, () -> McpServerLoader.load(file));

        assertTrue(ex.getMessage().contains("servers"));
    }

    @Test
    void illegalNameRejected() throws IOException {
        Path file = tempDir.resolve("mcp.json");
        Files.writeString(file, """
                {"servers": [{"name": "bad name", "command": "echo"}]}
                """);

        McpException ex = assertThrows(McpException.class, () -> McpServerLoader.load(file));

        assertTrue(ex.getMessage().contains("非法"));
    }
}
