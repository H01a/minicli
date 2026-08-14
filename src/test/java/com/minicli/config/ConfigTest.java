package com.minicli.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void parseEnvFileIgnoresCommentsAndBlankLines() throws IOException {
        Path env = tempDir.resolve(".env");
        Files.writeString(env, """
                # comment
                DEEPSEEK_API_KEY="sk-test-123"

                DEEPSEEK_MODEL=deepseek-v4-flash
                """);

        Map<String, String> values = Config.parseEnvFile(env);

        assertEquals("sk-test-123", values.get("DEEPSEEK_API_KEY"));
        assertEquals("deepseek-v4-flash", values.get("DEEPSEEK_MODEL"));
    }

    @Test
    void loadReadsKeyAndModelWithDefaultBaseUrl() throws IOException {
        Path env = tempDir.resolve(".env");
        Files.writeString(env, "DEEPSEEK_API_KEY=sk-test-123\nDEEPSEEK_MODEL=deepseek-v4-flash\n");

        Config config = Config.load(env);

        assertEquals("sk-test-123", config.apiKey());
        assertEquals("deepseek-v4-flash", config.model());
        assertEquals(Config.DEFAULT_BASE_URL, config.baseUrl());
    }

    @Test
    void loadFailsWhenApiKeyMissing() throws IOException {
        Path env = tempDir.resolve(".env");
        Files.writeString(env, "DEEPSEEK_MODEL=deepseek-v4-flash\n");

        assertThrows(ConfigException.class, () -> Config.load(env));
    }
}
