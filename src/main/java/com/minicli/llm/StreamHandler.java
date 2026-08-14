package com.minicli.llm;

/** 流式回答回调：输出增量、思维链增量、完成。 */
public interface StreamHandler {

    /** 最终回答的文本增量。 */
    void onOutputDelta(String delta);

    /** 思维链文本增量（默认忽略，实现可覆盖）。 */
    default void onReasoningDelta(String delta) {
    }

    /** 响应完成（response.completed）。 */
    void onDone();
}
