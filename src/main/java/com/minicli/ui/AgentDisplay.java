package com.minicli.ui;

import com.minicli.agent.core.AgentListener;
import com.minicli.llm.FunctionCall;
import com.minicli.tools.spi.ToolResult;
import org.jline.terminal.Terminal;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** 把 ReAct 过程事件渲染为终端过程块：思考块 → 工具调用块 → 流式最终回答。 */
public final class AgentDisplay implements AgentListener {

    private static final String RESET = "\u001b[0m";
    private static final String DIM = "\u001b[90m";
    private static final String CYAN = "\u001b[36m";

    private final Terminal terminal;
    private final StringBuilder reasoningBuffer = new StringBuilder();
    private long reasoningStartNanos;
    private boolean reasoningOpen;
    private boolean outputOpen;

    public AgentDisplay(Terminal terminal) {
        this.terminal = terminal;
    }

    @Override
    public void onReasoningDelta(String delta) {
        if (!reasoningOpen) {
            reasoningOpen = true;
            reasoningStartNanos = System.nanoTime();
        }
        reasoningBuffer.append(delta);
    }

    @Override
    public void onToolCallStarted(FunctionCall call) {
        flushThinking();
    }

    @Override
    public void onToolResult(FunctionCall call, ToolResult result, long durationMillis) {
        flushThinking();
        var writer = terminal.writer();
        writer.println();
        writer.println(CYAN + "🔧 invoke " + call.name() + RESET + " · running " + formatDuration(durationMillis));
        writer.println("  args:");
        printIndented(prettyJson(call.argumentsJson()), 4);
        writer.println("  output:");
        printIndented(prettyJson(result.isSuccess() ? result.output() : result.error()), 4);
        writer.flush();
    }

    @Override
    public void onOutputDelta(String delta) {
        flushThinking();
        if (!outputOpen) {
            terminal.writer().println();
            terminal.writer().print("👽> ");
            outputOpen = true;
        }
        terminal.writer().print(delta);
        terminal.writer().flush();
    }

    @Override
    public void onDone() {
        flushThinking();
        terminal.writer().println();
        terminal.writer().flush();
    }

    /** 思考结束（工具调用或最终回答开始时）把整块输出：标题含耗时，内容为实际 reasoning_text。 */
    private void flushThinking() {
        if (!reasoningOpen) {
            return;
        }
        long millis = (System.nanoTime() - reasoningStartNanos) / 1_000_000;
        reasoningOpen = false;
        var writer = terminal.writer();
        writer.println();
        writer.println(DIM + "🧠 thinking · thought " + formatDuration(millis) + RESET);
        printIndented(reasoningBuffer.toString(), 2);
        reasoningBuffer.setLength(0);
        writer.flush();
    }

    private void printIndented(String text, int spaces) {
        if (text == null || text.isBlank()) {
            terminal.writer().println(" ".repeat(spaces) + "(空)");
            return;
        }
        String prefix = " ".repeat(spaces);
        for (String line : text.split("\n", -1)) {
            terminal.writer().println(prefix + line);
        }
    }

    /** JSON 结构格式化；纯文本原样返回。 */
    private static String prettyJson(String s) {
        if (s == null || s.isBlank()) {
            return "(空)";
        }
        try {
            return new JSONObject(s).toString(2);
        } catch (JSONException ignored) {
        }
        try {
            return new JSONArray(s).toString(2);
        } catch (JSONException ignored) {
        }
        return s;
    }

    private static String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + "ms";
        }
        return String.format("%.1fs", millis / 1000.0);
    }
}
