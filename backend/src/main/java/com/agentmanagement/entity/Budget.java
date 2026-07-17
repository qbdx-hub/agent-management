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
 * 预算配置表实体（对应 budget 表）。
 * 按工作空间/用户/Agent 维度设置费用预算、告警阈值与超支熔断。
 * notifyChannels 为 JSON 列，用 JacksonTypeHandler；需 autoResultMap = true。
 */
@Data
@TableName(value = "budget", autoResultMap = true)
public class Budget implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long workspaceId;

    private String name;

    /** 范围：workspace/user/agent */
    private String scope;

    /** 范围 ID（用户 ID 或 Agent ID） */
    private Long scopeId;

    /** 周期：daily/monthly */
    private String period;

    /** 预算限额（美元） */
    private BigDecimal limitAmount;

    /** 当前已用（美元） */
    private BigDecimal currentAmount;

    /** 告警阈值（百分比） */
    private Integer warnPercent;

    /** 超支熔断是否启用：0-否 1-是 */
    private Integer meltdownEnabled;

    /** 通知渠道 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> notifyChannels;

    /** 是否启用：0-否 1-是 */
    private Integer enabled;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
