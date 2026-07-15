package com.agentmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Agent 工具绑定表实体（对应 agent_tool_binding 表）。
 * 一个 Agent 绑定多个工具的关联表，含启用状态与绑定配置。
 * config 为 JSON 列，用 JacksonTypeHandler；需 autoResultMap = true。
 */
@Data
@TableName(value = "agent_tool_binding", autoResultMap = true)
public class AgentToolBinding implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long agentId;

    private Long toolId;

    /** 是否启用：0-否 1-是 */
    private Integer enabled;

    /** 绑定配置 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> config;

    private LocalDateTime createdAt;
}
