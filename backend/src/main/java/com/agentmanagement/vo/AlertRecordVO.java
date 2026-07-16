package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 告警记录 VO —— 对齐前端 AlertRecord 类型。
 */
@Data
public class AlertRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long recordId;
    private String ruleName;
    private String severity;
    private String message;
    private String triggeredAt;
    private String resolvedAt;
    private String status;
}
