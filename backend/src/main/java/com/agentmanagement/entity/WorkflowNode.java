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
 * 工作流节点表实体（对应 workflow_node 表）。
 * 工作流中的单个节点，含类型、关联资源、配置与画布坐标。
 * config 为 JSON 列，用 JacksonTypeHandler；需 autoResultMap = true。
 */
@Data
@TableName(value = "workflow_node", autoResultMap = true)
public class WorkflowNode implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long workflowId;

    /** 节点唯一标识（前端生成） */
    private String nodeId;

    /** 节点类型：start/agent/tool/condition/end */
    private String type;

    private String label;

    private Long agentId;

    private Long toolId;

    /** 节点配置 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> config;

    /** X 坐标 */
    private Double positionX;

    /** Y 坐标 */
    private Double positionY;

    private LocalDateTime createdAt;
}
