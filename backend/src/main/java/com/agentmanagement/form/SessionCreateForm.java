package com.agentmanagement.form;

import lombok.Data;

import java.util.Map;

/**
 * 创建会话表单（对应前端 SessionCreateDTO）。
 */
@Data
public class SessionCreateForm {

    /** 会话标题，不传时后端自动生成 */
    private String title;

    /** 会话变量 */
    private Map<String, String> variables;

    /** 执行模式：auto/step_by_step/plan_only */
    private String executionMode;
}
