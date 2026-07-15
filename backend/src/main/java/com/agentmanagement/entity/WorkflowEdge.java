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
 * 工作流边表实体（对应 workflow_edge 表）。
 * 工作流节点之间的连线，含条件分支表达式。
 * condition 为 JSON 列，用 JacksonTypeHandler；需 autoResultMap = true。
 */
@Data
@TableName(value = "workflow_edge", autoResultMap = true)
public class WorkflowEdge implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long workflowId;

    /** 边唯一标识（前端生成） */
    private String edgeId;

    /** 源节点 ID */
    private String sourceNodeId;

    /** 目标节点 ID */
    private String targetNodeId;

    private String label;

    /** 条件表达式 JSON（condition 是 MySQL 保留字，列名需反引号转义，否则生成的 SQL 语法错误） */
    @TableField(value = "`condition`", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> condition;

    private LocalDateTime createdAt;
}
