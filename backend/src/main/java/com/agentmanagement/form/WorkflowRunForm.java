package com.agentmanagement.form;

import lombok.Data;

import java.util.Map;

/**
 * 工作流运行请求体。
 */
@Data
public class WorkflowRunForm {

    /** 运行输入（通常含 question：任务描述；condition 等节点可引用） */
    private Map<String, Object> input;
}
