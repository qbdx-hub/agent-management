package com.agentmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 执行步骤表实体（对应 execution_step 表）。
 * Agent 执行过程中的每一步（思考/工具调用/工具结果/反思/消息）。
 * toolInput / toolOutput 为 JSON 列，用 JacksonTypeHandler；需 autoResultMap = true。
 */
@Data
@TableName(value = "execution_step", autoResultMap = true)
public class ExecutionStep implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Long messageId;

    /** 步骤序号 */
    private Integer sequence;

    /** 类型：thinking/tool_call/tool_result/reflection/message */
    private String stepType;

    private String title;

    private String content;

    private String toolName;

    private String toolIcon;

    /** 工具输入 JSON（对应 request） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> toolInput;

    /** 工具输出 JSON（对应 response） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> toolOutput;

    /** 状态：pending/running/success/error/skipped */
    private String status;

    private String errorMessage;

    /** 重试次数 */
    private Integer retryCount;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    /** 耗时（ms） */
    private Integer durationMs;

    private LocalDateTime createdAt;
}
