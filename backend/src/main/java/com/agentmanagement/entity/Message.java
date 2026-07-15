package com.agentmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息表实体（对应 message 表）。
 * 会话中的每一条消息，含角色、内容、Token 与费用统计。
 * attachments 为 JSON 列，用 JacksonTypeHandler；需 autoResultMap = true。
 */
@Data
@TableName(value = "message", autoResultMap = true)
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    /** 角色：user/assistant/system/tool */
    private String role;

    private String content;

    /** 发送时执行模式：auto/step_by_step/plan_only */
    private String mode;

    /** 附件路径 JSON 数组 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> attachments;

    private Long tokenInput;

    private Long tokenOutput;

    private Long tokenTotal;

    /** 费用（美元） */
    private BigDecimal tokenCost;

    /** 延迟（ms） */
    private Integer latency;

    private LocalDateTime createdAt;
}
