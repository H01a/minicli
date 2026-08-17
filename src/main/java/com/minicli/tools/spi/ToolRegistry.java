package com.minicli.tools.spi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 统一工具注册表：内置工具启动时注册，MCP 工具将来动态注册进同一注册表。 */
public final class ToolRegistry {

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public void register(Tool tool) {
        String name = tool.name();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("工具名不能为空");
        }
        if (tools.containsKey(name)) {
            throw new IllegalArgumentException("工具已注册: " + name);
        }
        tools.put(name, tool);
    }

    public Optional<Tool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public Tool require(String name) {
        return find(name).orElseThrow(() -> new IllegalArgumentException("未注册的工具: " + name));
    }

    /** 按注册顺序返回全部工具。 */
    public List<Tool> all() {
        return new ArrayList<>(tools.values());
    }

    public int size() {
        return tools.size();
    }
}
