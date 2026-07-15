package com.agentmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 费用记录表实体（对应 cost_record 表）。
 * 每次 LLM 调用产生的 Token 与费用明细，用于成本统计与归因。
 */
@Data
@TableName("cost_record")
public class CostRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long workspaceId;

    private Long agentId;

    /** Agent 名称（冗余） */
    private String agentName;

    private Long sessionId;

    private String modelProvider;

    private String modelName;

    private Long tokenInput;

    private Long tokenOutput;

    private Long totalTokens;

    /** 费用（美元） */
    private BigDecimal cost;

    private Long userId;

    /** 用户名（冗余） */
    private String userName;

    /** 记录时间 */
    private LocalDateTime recordedAt;
}
