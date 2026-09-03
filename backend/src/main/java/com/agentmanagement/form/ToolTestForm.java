package com.agentmanagement.form;

import lombok.Data;

import java.util.Map;

/**
 * 工具连通性测试请求体（前端发送 { parameters: {...} }）。
 */
@Data
public class ToolTestForm {

    /** 测试参数（key → value，按工具参数定义填充） */
    private Map<String, Object> parameters;
}
