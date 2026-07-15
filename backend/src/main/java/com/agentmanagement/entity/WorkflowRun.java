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
import java.util.Map;

/**
 * 工作流运行记录表实体（对应 workflow_run 表）。
 * 工作流每次执行的输入、输出、各节点结果、Token/费用与耗时统计。
 * input/output/nodeResults 为 JSON 列，用 JacksonTypeHandler；需 autoResultMap = true。
 */
@Data
@TableName(value = "workflow_run", autoResultMap = true)
public class WorkflowRun implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long workflowId;

    /** 状态：pending/running/completed/failed */
    private String status;

    /** 输入 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> input;

    /** 输出 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> output;

    /** 各节点结果 JSON 数组 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> nodeResults;

    /** 错误信息 */
    private String error;

    private Long totalTokens;

    /** 总费用（美元） */
    private BigDecimal totalCost;

    /** 总耗时（ms） */
    private Integer duration;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    /** 触发者 ID */
    private Long triggeredBy;

    private LocalDateTime createdAt;
}
