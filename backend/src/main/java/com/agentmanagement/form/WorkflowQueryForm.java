package com.agentmanagement.form;

import lombok.Data;

/**
 * 工作流列表查询表单。
 */
@Data
public class WorkflowQueryForm {

    private Integer page = 1;

    private Integer pageSize = 20;

    /** 按 name 模糊搜索 */
    private String keyword;

    /** 状态筛选：draft/active/archived */
    private String status;
}
