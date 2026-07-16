package com.agentmanagement.vo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 成本趋势 VO（按天/小时聚合）。
 */
@Data
public class CostTrendVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String date;
    private BigDecimal cost;
    private Long tokens;
}
