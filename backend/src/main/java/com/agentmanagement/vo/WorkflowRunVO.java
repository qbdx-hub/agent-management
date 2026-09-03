package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工作流运行记录 VO（对应前端运行详情/历史列表）。
 * nodeResults 每项：{nodeId, label, type, status, output, error, durationMs, sequence}。
 */
@Data
public class WorkflowRunVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long workflowId;

    /** 状态：running/waiting_approval/completed/failed */
    private String status;

    private Map<String, Object> input;

    private Map<String, Object> output;

    private List<Map<String, Object>> nodeResults;

    private String error;

    private Long totalTokens;

    private BigDecimal totalCost;

    private Integer duration;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private Long triggeredBy;

    private String triggeredByName;

    private LocalDateTime createdAt;
}
