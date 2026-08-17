package com.minicli.agent.core;

import com.minicli.llm.FunctionCall;
import com.minicli.tools.spi.ToolResult;

/**
 * ReAct 循环过程事件监听：REPL 用它做过程展示（reasoning/工具链路/流式最终回答）。
 * 全部方法默认空实现，展示方按需覆盖。
 */
public interface AgentListener {

    /** 每轮循环开始（step 从 1 开始）。 */
    default void onStep(int step, int maxSteps) {
    }

    /** 思维链文本增量。 */
    default void onReasoningDelta(String delta) {
    }

    /** 本轮开始执行一个工具调用。 */
    default void onToolCallStarted(FunctionCall call) {
    }

    /** 一个工具执行结束（durationMillis 为实际执行耗时；output/error 可能较长，展示方自行截断）。 */
    default void onToolResult(FunctionCall call, ToolResult result, long durationMillis) {
    }

    /** 最终回答文本增量（流式）。 */
    default void onOutputDelta(String delta) {
    }

    /** 任务结束（已收到最终回答）。 */
    default void onDone() {
    }
}
