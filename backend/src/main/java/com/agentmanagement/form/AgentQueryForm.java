package com.agentmanagement.form;

import lombok.Data;

/**
 * Agent 列表查询表单（controller 参数对象绑定 query 参数）。
 * 分页字段 page/pageSize 对齐前端 PaginationQuery；keyword/status/tag 为筛选条件。
 */
@Data
public class AgentQueryForm {

    private Integer page = 1;

    private Integer pageSize = 20;

    /** 按 name/description 模糊搜索 */
    private String keyword;

    /** 状态筛选：draft/testing/published/paused/archived */
    private String status;

    /** 标签筛选（可选） */
    private String tag;
}
