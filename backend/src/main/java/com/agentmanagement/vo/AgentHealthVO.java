package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * Agent 健康排行 VO —— 对齐前端 AgentHealthItem 类型。
 */
@Data
public class AgentHealthVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long agentId;
    private String agentName;
    private String status;  // healthy / warning / critical
    private Double successRate;
    private Integer avgLatencyMs;
    private Integer callCount24h;
    private String errorSummary;
}
