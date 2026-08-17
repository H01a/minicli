package com.minicli.tools.spi;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolRegistryTest {

    private static final Tool TOOL_A = new Tool() {
        @Override public String name() { return "a"; }
        @Override public String description() { return "A"; }
        @Override public JSONObject inputSchema() { return new JSONObject(); }
        @Override public ToolResult invoke(JSONObject args) { return ToolResult.success("ok"); }
    };

    @Test
    void registerAndFind() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(TOOL_A);
        assertTrue(registry.find("a").isPresent());
        assertEquals(TOOL_A, registry.require("a"));
        assertEquals(1, registry.size());
        assertEquals(List.of(TOOL_A), registry.all());
    }

    @Test
    void findMissingReturnsEmpty() {
        ToolRegistry registry = new ToolRegistry();
        assertTrue(registry.find("nope").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> registry.require("nope"));
    }

    @Test
    void duplicateNameRejected() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(TOOL_A);
        assertThrows(IllegalArgumentException.class, () -> registry.register(TOOL_A));
        assertEquals(1, registry.size());
    }
}
