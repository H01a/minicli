package com.minicli.tools.builtin;

import com.minicli.tools.spi.ToolResult;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetCwdToolTest {

    @Test
    void returnsUserDir() {
        ToolResult result = new GetCwdTool().invoke(new JSONObject());
        assertTrue(result.isSuccess());
        assertEquals(System.getProperty("user.dir"), result.output());
    }
}
