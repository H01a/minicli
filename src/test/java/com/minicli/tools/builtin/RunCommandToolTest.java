package com.minicli.tools.builtin;

import com.minicli.tools.spi.ToolResult;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunCommandToolTest {

    @Test
    void runsCommandAndReturnsOutput() {
        ToolResult result = new RunCommandTool().invoke(new JSONObject().put("command", "echo hi"));
        assertTrue(result.isSuccess());
        assertTrue(result.output().contains("exit=0"));
        assertTrue(result.output().contains("hi"));
    }

    @Test
    void nonZeroExitStillReturnsSuccess() {
        ToolResult result = new RunCommandTool().invoke(new JSONObject().put("command", "exit 3"));
        assertTrue(result.isSuccess());
        assertTrue(result.output().contains("exit=3"));
    }

    @Test
    void timesOut() {
        ToolResult result = new RunCommandTool().invoke(new JSONObject()
                .put("command", "sleep 5")
                .put("timeoutSeconds", 1));
        assertFalse(result.isSuccess());
        assertTrue(result.error().contains("超时"));
    }

    @Test
    void missingCommandFails() {
        ToolResult result = new RunCommandTool().invoke(new JSONObject());
        assertFalse(result.isSuccess());
        assertTrue(result.error().contains("缺少必填参数 command"));
    }
}
