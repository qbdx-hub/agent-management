package com.agentmanagement.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建工作流表单（列表页「创建工作流」用，创建一个 draft 空画布）。
 */
@Data
public class WorkflowCreateForm {

    @NotBlank(message = "工作流名称不能为空")
    @Size(max = 100, message = "工作流名称不能超过100字")
    private String name;

    @Size(max = 500, message = "描述不能超过500字")
    private String description;
}
