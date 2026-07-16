package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 监控概览 VO —— 对齐前端 MonitorOverview 类型。
 */
@Data
public class MonitorOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer activeAgentCount;
    private Integer runningTaskCount;
    private Long todayCallCount;
    private Double successRate;
    private Integer avgLatencyMs;
    private Integer p99LatencyMs;
    private Long totalTokensToday;

    /** 趋势（环比变化百分比） */
    private Trends trends;

    @Data
    public static class Trends implements Serializable {
        private Double callCountChange;
        private Double successRateChange;
        private Double latencyChange;
    }
}
