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
 * 工具调用记录表实体（对应 tool_call_record 表）。
 * 记录每次工具调用的参数、结果、耗时与错误信息。
 * params 为 JSON 列，用 JacksonTypeHandler；需 autoResultMap = true。
 */
@Data
@TableName(value = "tool_call_record", autoResultMap = true)
public class ToolCallRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long toolId;

    private Long agentId;

    private Long sessionId;

    private Long stepId;

    /** 调用参数 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> params;

    /** 结果摘要 */
    private String resultSummary;

    /** 是否成功：0-否 1-是 */
    private Integer success;

    /** 延迟（ms） */
    private Integer latencyMs;

    private String errorMessage;

    private LocalDateTime createdAt;
}
