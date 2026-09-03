package com.agentmanagement.service.builtin;

/**
 * 内置工具（builtin）执行结果：成功时 output 为回传给模型的文本，失败时 errorMessage 给出可读原因。
 */
public class BuiltinToolResult {

    private boolean success;
    private String output;
    private String errorMessage;

    public static BuiltinToolResult ok(String output) {
        BuiltinToolResult r = new BuiltinToolResult();
        r.success = true;
        r.output = output != null ? output : "";
        return r;
    }

    public static BuiltinToolResult fail(String errorMessage) {
        BuiltinToolResult r = new BuiltinToolResult();
        r.success = false;
        r.errorMessage = errorMessage != null && !errorMessage.isEmpty() ? errorMessage : "内置工具执行失败";
        return r;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
