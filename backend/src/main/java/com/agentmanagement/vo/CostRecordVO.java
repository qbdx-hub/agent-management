package com.agentmanagement.vo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 费用记录 VO（对应前端 CostRecord）。
 */
@Data
public class CostRecordVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long recordId;
    private Long agentId;
    private String agentName;
    private Long sessionId;
    private String modelProvider;
    private String modelName;
    private Long tokenInput;
    private Long tokenOutput;
    private BigDecimal cost;
    private Long userId;
    private String userName;
    private LocalDateTime createdAt;
}
