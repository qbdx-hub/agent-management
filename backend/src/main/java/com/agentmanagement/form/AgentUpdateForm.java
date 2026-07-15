package com.agentmanagement.form;

import lombok.Data;

import javax.validation.constraints.Size;
import java.util.List;

/**
 * 编辑 Agent 基本信息表单（对应前端 UpdateAgentDTO，字段全部可选）。
 * 仅更新 name/description/avatar/tags；模型/提示词/记忆等配置走各自子接口（本期不接通）。
 */
@Data
public class AgentUpdateForm {

    @Size(max = 100, message = "Agent名称不能超过100字")
    private String name;

    @Size(max = 500, message = "描述不能超过500字")
    private String description;

    private String avatar;

    private List<String> tags;
}
