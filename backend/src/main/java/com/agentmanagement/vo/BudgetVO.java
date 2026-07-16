package com.agentmanagement.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预算配置响应 VO。
 * 字段名与前端 BudgetConfig 对齐：limit / currentAmount / enabled 用前端类型。
 */
@Data
public class BudgetVO {

    private Long id;

    private String name;

    /** 范围：workspace/user/agent */
    private String scope;

    /** 范围 ID */
    private Long scopeId;

    /** 周期：daily/monthly */
    private String period;

    /** 预算限额（前端字段名 limit，对应后端 limitAmount） */
    private BigDecimal limit;

    /** 当前已用 */
    private BigDecimal currentAmount;

    /** 告警阈值百分比 */
    private Integer warnPercent;

    /** 超支熔断是否启用 */
    private Boolean meltdownEnabled;

    /** 通知渠道列表 */
    private List<String> notifyChannels;

    /** 是否启用 */
    private Boolean enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
