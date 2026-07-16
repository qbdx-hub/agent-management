package com.agentmanagement.vo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 成本明细 VO（按 model/agent/member 分组）。
 */
@Data
public class CostBreakdownVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String label;
    private BigDecimal cost;
    private BigDecimal percent;
    private Long tokenInput;
    private Long tokenOutput;
}
