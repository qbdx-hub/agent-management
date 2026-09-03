package com.agentmanagement.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 创建工作空间请求体。
 */
@Data
public class WorkspaceCreateForm {

    @NotBlank(message = "空间名称不能为空")
    private String name;

    private String description;
}
