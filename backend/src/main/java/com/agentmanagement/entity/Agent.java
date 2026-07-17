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
 * Agent 表实体（对应 agent 表）。
 * 含模型配置、提示词配置、记忆配置、执行配置、统计冗余字段。
 * tags / promptVariables / knowledgeBaseIds / outputSchema 为 JSON 列，
 * 用 JacksonTypeHandler 自动序列化；必须 autoResultMap = true 否则 typeHandler 不触发。
 */
@Data
@TableName(value = "agent", autoResultMap = true)
public class Agent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long workspaceId;

    private String name;

    private String displayName;

    private String description;

    private String avatar;

    /** 状态：draft/testing/published/paused/archived */
    private String status;

    /** 标签 JSON 数组 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    // ============ 模型配置 ============

    private String modelProvider;

    private String modelName;

    /** 温度 */
    private BigDecimal temperature;

    private Integer maxTokens;

    /** Top P */
    private BigDecimal topP;

    // ============ AI 连接配置 ============

    /** AI API Base URL（如 https://api.openai.com/v1） */
    private String aiBaseUrl;

    /** AI API Key */
    private String aiApiKey;

    /** AI 模型名称（如 gpt-4o, deepseek-chat） */
    private String aiModel;

    // ============ 提示词配置 ============

    private String systemPrompt;

    /** 提示词变量 JSON 数组（每项 {name,label,type,options,defaultValue,required}） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> promptVariables;

    // ============ 记忆配置 ============

    /** 记忆策略：buffer/summary/vector/sliding_window/full */
    private String memoryStrategy;

    /** 工作窗口（消息轮数） */
    private Integer workingWindow;

    /** 是否启用长期记忆：0-否 1-是 */
    private Integer longTermEnabled;

    /** 绑定知识库 ID 数组 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> knowledgeBaseIds;

    // ============ 执行配置 ============

    /** 执行模式：auto/semi/manual */
    private String executionMode;

    private Integer maxIterations;

    /** 超时时间（ms） */
    private Integer timeout;

    /** 是否启用反思：0-否 1-是 */
    private Integer reflectionEnabled;

    private Integer reflectionDepth;

    /** 输出格式 Schema JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> outputSchema;

    // ============ Token 价格配置（美元 / 百万 token） ============

    /** 输入 token 单价（美元/百万 token） */
    private BigDecimal inputPricePerMillion;

    /** 缓存命中输入 token 单价（美元/百万 token） */
    private BigDecimal cachedInputPricePerMillion;

    /** 输出 token 单价（美元/百万 token） */
    private BigDecimal outputPricePerMillion;

    // ============ 统计（冗余字段，定期同步） ============

    private Long totalSessions;

    private Long totalMessages;

    private Long totalTokens;

    /** 总费用（美元） */
    private BigDecimal totalCost;

    /** 成功率（%） */
    private BigDecimal successRate;

    /** 平均延迟（ms） */
    private Integer avgLatencyMs;

    // ============ 审计 ============

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
