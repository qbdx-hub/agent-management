package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 工具调用统计 VO（对应前端 ToolStats），由 tool_call_record 实时聚合。
 */
@Data
public class ToolStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long totalCalls;

    /** 成功率 0-1 */
    private Double successRate;

    private Integer avgLatencyMs;

    private Integer p99LatencyMs;

    /** 近 14 天逐日调用/失败数 */
    private List<DailyCall> dailyCallCount;

    /** 调用最多的 Agent TOP5 */
    private List<AgentCall> topAgents;

    @Data
    public static class DailyCall implements Serializable {
        private String date;
        private Long count;
        private Long failCount;
    }

    @Data
    public static class AgentCall implements Serializable {
        private Long agentId;
        private String agentName;
        private Long callCount;
    }
}
