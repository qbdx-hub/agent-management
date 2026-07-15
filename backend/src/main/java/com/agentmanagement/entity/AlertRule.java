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
import java.util.List;

/**
 * 告警规则表实体（对应 alert_rule 表）。
 * 按指标 + 条件 + 阈值 + 持续时间触发告警。
 * notifyChannels 为 JSON 列，用 JacksonTypeHandler；需 autoResultMap = true。
 */
@Data
@TableName(value = "alert_rule", autoResultMap = true)
public class AlertRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long workspaceId;

    private String name;

    /** 指标：success_rate/p99_latency/daily_tokens/error_rate/cost */
    private String metric;

    /** 目标类型：workspace/agent */
    private String targetType;

    /** 目标 ID（NULL=整个工作空间） */
    private Long targetId;

    /** 条件：lt/gt/lte/gte（condition 是 MySQL 保留字，列名需反引号转义） */
    @TableField(value = "`condition`")
    private String condition;

    /** 阈值 */
    private BigDecimal threshold;

    /** 持续时间：0m/5m/1h/1d */
    private String duration;

    /** 级别：info/warning/critical */
    private String severity;

    /** 是否启用：0-否 1-是 */
    private Integer enabled;

    /** 通知渠道 JSON：["feishu","email","webhook"] */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> notifyChannels;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
