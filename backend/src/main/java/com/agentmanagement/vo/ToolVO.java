package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工具详情 VO（对应前端 ToolDetail）。
 * entity 扁平字段（endpointUrl/method/headers/endpointTimeout）组装为前端期望的嵌套 endpoint。
 */
@Data
public class ToolVO implements Serializable {

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

    private Endpoint endpoint;

    private List<Map<String, Object>> parameters;

    private String responseMapping;

    private String credentialRef;

    private Boolean retryOnFail;

    private Integer maxRetries;

    private Integer bindAgentCount;

    private Long totalCalls;

    private BigDecimal successRate;

    private Integer avgLatencyMs;

    private List<ToolCallRecordVO> recentCalls;

    private LocalDateTime createdAt;

    @Data
    public static class Endpoint {
        private String url;
        private String method;
        private Map<String, String> headers;
        private Integer timeoutMs;
    }

    @Data
    public static class ToolCallRecordVO {
        private String time;
        private Map<String, Object> params;
        private String resultSummary;
        private Boolean success;
        private Integer latencyMs;
    }
}
