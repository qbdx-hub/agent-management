package com.agentmanagement.form;

import lombok.Data;

/**
 * 工具列表查询表单（对象绑定 query 参数）。
 */
@Data
public class ToolQueryForm {

    private Integer page = 1;

    private Integer pageSize = 20;

    /** 按 name/displayName 模糊搜索 */
    private String keyword;

    /** 分类筛选 */
    private String category;
}
