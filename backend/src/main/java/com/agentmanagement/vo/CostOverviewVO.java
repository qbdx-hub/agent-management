package com.agentmanagement.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 成本概览响应 VO。
 */
@Data
public class CostOverviewVO {

    private BigDecimal totalCost;

    private BigDecimal budgetLimit;

    private BigDecimal budgetRemaining;

    private BigDecimal budgetPercent;

    private BigDecimal todayCost;

    private BigDecimal yesterdayCost;

    private BigDecimal projectedMonthCost;

    /** 熔断状态：normal / warning / meltdown */
    private String meltdownStatus;
}
