package com.agentmanagement.form;

import lombok.Data;

/**
 * 审批处理请求体（审批中心）。
 */
@Data
public class ApprovalDecisionForm {

    /** 审批意见 */
    private String comment;
}
