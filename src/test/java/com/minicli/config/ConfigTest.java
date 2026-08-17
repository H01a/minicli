package com.minicli.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void loadAppliesDefaultsForNewConfigKeys() throws IOException {
        Path env = tempDir.resolve(".env");
        Files.writeString(env, "DEEPSEEK_API_KEY=sk-test-123\nDEEPSEEK_MODEL=deepseek-v4-flash\n");

        Config config = Config.load(env);

        assertEquals(Config.DEFAULT_BASE_URL, config.baseUrl());
        assertEquals(Config.DEFAULT_CONNECT_TIMEOUT_SECONDS, config.connectTimeoutSeconds());
        assertEquals(Config.DEFAULT_READ_TIMEOUT_SECONDS, config.readTimeoutSeconds());
        assertEquals(Config.DEFAULT_MAX_STEPS, config.maxSteps());
        assertEquals(Config.DEFAULT_MAX_CONCURRENCY, config.maxConcurrency());
        assertEquals(Config.DEFAULT_MAX_OBSERVATION_CHARS, config.maxObservationChars());
    }

    @Test
    void loadReadsCustomValues() throws IOException {
        Path env = tempDir.resolve(".env");
        Files.writeString(env, """
                DEEPSEEK_API_KEY=sk-test-123
                DEEPSEEK_MODEL=deepseek-v4-flash
                DEEPSEEK_BASE_URL=http://localhost:9999/v1
                DEEPSEEK_CONNECT_TIMEOUT_SECONDS=5
                DEEPSEEK_READ_TIMEOUT_SECONDS=60
                MINICLI_AGENT_MAX_STEPS=3
                MINICLI_AGENT_MAX_CONCURRENCY=2
                MINICLI_AGENT_MAX_OBSERVATION_CHARS=100
                """);

        Config config = Config.load(env);

        assertEquals("http://localhost:9999/v1", config.baseUrl());
        assertEquals(5, config.connectTimeoutSeconds());
        assertEquals(60, config.readTimeoutSeconds());
        assertEquals(3, config.maxSteps());
        assertEquals(2, config.maxConcurrency());
        assertEquals(100, config.maxObservationChars());
    }

    @Test
    void loadFailsOnInvalidPositiveInt() throws IOException {
        Path env = tempDir.resolve(".env");
        Files.writeString(env, """
                DEEPSEEK_API_KEY=sk-test-123
                DEEPSEEK_MODEL=deepseek-v4-flash
                MINICLI_AGENT_MAX_STEPS=0
                """);

        ConfigException ex = assertThrows(ConfigException.class, () -> Config.load(env));

        assertTrue(ex.getMessage().contains("MINICLI_AGENT_MAX_STEPS"));
    }
}
