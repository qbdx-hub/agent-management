package com.agentmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审批记录表实体（对应 approval 表）。
 * 一次审批申请及其审批结果。
 */
@Data
@TableName("approval")
public class Approval implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long workspaceId;

    private Long ruleId;

    /** 规则名称（冗余） */
    private String ruleName;

    private String resourceType;

    private Long resourceId;

    private String resourceName;

    /** 操作 */
    private String action;

    /** 详情 */
    private String detail;

    private Long applicantId;

    /** 申请人名称（冗余） */
    private String applicantName;

    private Long approverId;

    /** 审批人名称（冗余） */
    private String approverName;

    /** 状态：pending/approved/rejected */
    private String status;

    /** 审批意见 */
    private String reason;

    /** 处理时间 */
    private LocalDateTime resolvedAt;

    private LocalDateTime createdAt;
}
