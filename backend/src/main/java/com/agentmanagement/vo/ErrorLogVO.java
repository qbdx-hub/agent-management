package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 错误日志 VO —— 对齐前端 ErrorLogItem 类型。
 */
@Data
public class ErrorLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long errorId;
    private Long agentId;
    private String agentName;
    private Long sessionId;
    private Integer stepSequence;
    private String errorType;
    private String errorMessage;
    private String occurredAt;
}
