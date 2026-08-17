package com.minicli.tools.spi;

/** 工具执行结果：成功携带输出文本，失败携带错误信息。耗时由审计层记录，不在此模型内。 */
public record ToolResult(Status status, String output, String error) {

    public enum Status { SUCCESS, FAILURE }

    public static ToolResult success(String output) {
        return new ToolResult(Status.SUCCESS, output, null);
    }

    public static ToolResult failure(String error) {
        return new ToolResult(Status.FAILURE, null, error);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
