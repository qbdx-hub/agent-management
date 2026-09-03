package com.agentmanagement.form;

import lombok.Data;

/**
 * 工作流审批处理请求体。
 */
@Data
public class WorkflowApproveForm {

    /** true=通过（继续执行后续节点），false=拒绝（运行置为失败） */
    private Boolean approved;

    /** 审批意见 */
    private String reason;
}
