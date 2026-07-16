package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会话列表项 VO（对应前端 SessionSummary）。
 */
@Data
public class SessionSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long sessionId;

    private String title;

    /** 状态：active/completed/stopped/error */
    private String status;

    private Integer messageCount;

    private Long totalTokens;

    private BigDecimal totalCost;

    private LocalDateTime lastMessageAt;

    private LocalDateTime createdAt;
}
