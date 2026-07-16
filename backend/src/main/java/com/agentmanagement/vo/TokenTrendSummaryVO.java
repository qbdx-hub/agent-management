package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * Token 趋势返回结构（含 series + summary）—— 对齐前端 getTokenTrend 返回。
 */
@Data
public class TokenTrendSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private java.util.List<TokenTrendPointVO> series;
    private Summary summary;

    @Data
    public static class Summary implements Serializable {
        private Long totalInput;
        private Long totalOutput;
        private Double totalCost;
    }
}
