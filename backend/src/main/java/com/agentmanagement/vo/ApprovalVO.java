package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审批记录 VO（审批中心列表行，对应前端 ApprovalItem）。
 */
@Data
public class ApprovalVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long approvalId;

    private Long ruleId;

    private String ruleName;

    private String resourceType;

    private Long resourceId;

    private String resourceName;

    private String action;

    private String detail;

    private Long applicantId;

    private String applicantName;

    private String approverName;

    /** 状态：pending/approved/rejected */
    private String status;

    private String reason;

    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;
}
