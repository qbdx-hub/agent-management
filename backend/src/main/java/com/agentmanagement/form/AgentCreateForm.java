package com.agentmanagement.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 新建 Agent 表单（对应前端 CreateAgentDTO + 模型配置）。
 * 前端创建向导第 2 步采集的模型配置一并提交，后端创建时直接落库。
 */
@Data
public class AgentCreateForm {

    @NotBlank(message = "Agent名称不能为空")
    @Size(max = 100, message = "Agent名称不能超过100字")
    private String name;

    @Size(max = 500, message = "描述不能超过500字")
    private String description;

    private String avatar;

    private List<String> tags;

    /** 状态：draft/testing/published/paused/archived；不传时后端默认 draft */
    private String status;

    // ===== 模型配置（创建时一并落库） =====

    private String modelProvider;

    private String modelName;

    private BigDecimal temperature;

    private Integer maxTokens;
}
