package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 监控图表 VO —— 调用/错误趋势、费用趋势、Agent 调用分布、错误类型分布。
 * 桶粒度与 Token 趋势一致：today 按小时（24 桶），7d/30d 按天。
 */
@Data
public class MonitorChartsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 调用与错误趋势（calls=成功调用 cost_record 数，errors=error_log 数） */
    private List<CallTrendPoint> callTrend;

    /** 费用趋势（各桶真实费用合计，元） */
    private List<CostTrendPoint> costTrend;

    /** Agent 调用分布 TOP5（窗口内按调用次数） */
    private List<AgentCallDist> agentDistribution;

    /** 错误类型分布（窗口内按 error_type 分组） */
    private List<ErrorTypeDist> errorTypeDistribution;

    @Data
    public static class CallTrendPoint implements Serializable {
        private String time;
        private Long calls;
        private Long errors;
    }

    @Data
    public static class CostTrendPoint implements Serializable {
        private String time;
        private Double cost;
    }

    @Data
    public static class AgentCallDist implements Serializable {
        private Long agentId;
        private String agentName;
        private Long calls;
        private Long tokens;
    }

    @Data
    public static class ErrorTypeDist implements Serializable {
        private String errorType;
        private Long count;
    }
}
