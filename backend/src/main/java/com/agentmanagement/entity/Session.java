package com.agentmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 会话表实体（对应 session 表）。
 * 一个 Agent 与用户的一次完整对话会话，含 Token/费用/延迟统计。
 * variables 为 JSON 列，用 JacksonTypeHandler；需 autoResultMap = true。
 */
@Data
@TableName(value = "session", autoResultMap = true)
public class Session implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long workspaceId;

    private Long agentId;

    private String title;

    /** 状态：active/completed/stopped/error */
    private String status;

    /** 执行模式：auto/step_by_step/plan_only */
    private String executionMode;

    /** 会话变量 JSON（key-value） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> variables;

    private Long totalTokens;

    /** 总费用（美元） */
    private BigDecimal totalCost;

    /** 总延迟（ms） */
    private Integer latency;

    private Integer messageCount;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private Long createdBy;

    private LocalDateTime createdAt;
}
