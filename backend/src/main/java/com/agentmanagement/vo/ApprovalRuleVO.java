package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 审批规则 VO（对应前端 ApprovalRule）。
 */
@Data
public class ApprovalRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    /** 触发操作：publish/register/delete */
    private String triggerAction;

    /** 触发条件描述（JSON 转文本展示） */
    private String triggerCondition;

    private String approverRole;

    private Integer requiredApprovals;

    private Boolean enabled;
}
