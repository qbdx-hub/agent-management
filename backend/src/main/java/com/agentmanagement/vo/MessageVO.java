package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 消息 VO（对应前端 Message 接口）。
 */
@Data
public class MessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long messageId;

    /** 角色：user/assistant/system */
    private String role;

    private String content;

    /** 执行步骤（assistant 消息才有） */
    private List<ExecutionStepVO> steps;

    /** Token 用量 */
    private TokenUsageVO tokenUsage;

    private LocalDateTime createdAt;

    @Data
    public static class ExecutionStepVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long stepId;
        private Integer sequence;
        /** 类型：thinking/tool_call/tool_result */
        private String type;
        /** 状态：pending/running/success/error/skipped */
        private String status;
        private String content;
        private String toolName;
        private String toolIcon;
        private Map<String, Object> request;
        private Map<String, Object> response;
        private String errorMessage;
        private Integer retryCount;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private Integer durationMs;
    }

    @Data
    public static class TokenUsageVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long input;
        private Long output;
        private Long total;
        private BigDecimal cost;
    }
}
