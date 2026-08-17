package com.minicli.tools.builtin;

import com.minicli.tools.spi.ToolResult;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemInfoToolTest {

    @Test
    void reportsSystemInfo() {
        ToolResult result = new SystemInfoTool().invoke(new JSONObject());
        assertTrue(result.isSuccess());
        assertTrue(result.output().contains(System.getProperty("os.name")));
        assertTrue(result.output().contains(System.getProperty("java.version")));
    }
}
