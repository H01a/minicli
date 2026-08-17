package com.minicli.tools.builtin;

import com.minicli.tools.spi.ToolResult;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetEnvToolTest {

    @Test
    void readsExistingVariable() {
        String path = System.getenv("PATH");
        assertNotNull(path);

        ToolResult result = new GetEnvTool().invoke(new JSONObject().put("name", "PATH"));

        assertTrue(result.isSuccess());
        assertTrue(result.output().startsWith("PATH="));
    }

    @Test
    void listsAllNames() {
        ToolResult result = new GetEnvTool().invoke(new JSONObject());
        assertTrue(result.isSuccess());
        assertTrue(result.output().contains("PATH"));
    }

    @Test
    void missingVariableFails() {
        ToolResult result = new GetEnvTool().invoke(new JSONObject().put("name", "MINICLI_DEFINITELY_NOT_SET_XYZ"));
        assertFalse(result.isSuccess());
        assertTrue(result.error().contains("环境变量不存在"));
    }
}
