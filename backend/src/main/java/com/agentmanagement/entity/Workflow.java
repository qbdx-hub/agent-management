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
 * 工作流表实体（对应 workflow 表）。
 * 节点编排后的可运行工作流，含触发方式与配置。
 * triggerConfig 为 JSON 列，用 JacksonTypeHandler；需 autoResultMap = true。
 */
@Data
@TableName(value = "workflow", autoResultMap = true)
public class Workflow implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long workspaceId;

    private String name;

    private String description;

    /** 状态：draft/active/archived */
    private String status;

    /** 触发方式：manual/scheduled/event */
    private String triggerType;

    /** 触发配置 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> triggerConfig;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
