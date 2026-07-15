package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 列表项 VO（对应前端 AgentSummary）。
 */
@Data
public class AgentSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String description;

    private String avatar;

    private String status;

    private String modelName;

    private List<String> tags;

    /** 绑定工具数（工具绑定子接口未接通前固定 0） */
    private Integer toolCount;

    private Long totalSessions;

    private BigDecimal successRate;

    private Integer avgLatencyMs;

    private Long createdBy;

    private String creatorName;

    private LocalDateTime updatedAt;
}
