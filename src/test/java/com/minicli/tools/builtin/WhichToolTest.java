package com.minicli.tools.builtin;

import com.minicli.tools.spi.ToolResult;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhichToolTest {

    @Test
    void findsCommandInPath() {
        ToolResult result = new WhichTool().invoke(new JSONObject().put("command", "sh"));
        assertTrue(result.isSuccess());
        assertTrue(result.output().contains("sh"));
    }

    @Test
    void missingCommandFails() {
        ToolResult result = new WhichTool().invoke(new JSONObject().put("command", "definitely-no-such-cmd-xyz-123"));
        assertFalse(result.isSuccess());
        assertTrue(result.error().contains("未找到"));
    }
}
