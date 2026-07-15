package com.agentmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 错误日志表实体（对应 error_log 表，监控模块）。
 * 记录 Agent/会话/步骤执行过程中的错误信息与堆栈。
 */
@Data
@TableName("error_log")
public class ErrorLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long workspaceId;

    private Long agentId;

    /** Agent 名称（冗余） */
    private String agentName;

    private Long sessionId;

    private Long stepId;

    /** 错误类型：tool_timeout/db_timeout/api_error/llm_error/unknown */
    private String errorType;

    private String errorMessage;

    /** 堆栈跟踪 */
    private String stackTrace;

    /** 发生时间 */
    private LocalDateTime occurredAt;
}
