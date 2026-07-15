package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工具列表项 VO（对应前端 ToolSummary）。
 */
@Data
public class ToolSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String displayName;

    private String description;

    private String category;

    private String categoryLabel;

    private String icon;

    private String type;

    private String status;

    private Integer bindAgentCount;

    private Long totalCalls;

    private BigDecimal successRate;

    private Integer avgLatencyMs;

    private LocalDateTime createdAt;
}
