package com.agentmanagement.form;

import lombok.Data;

import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 编辑 Agent 表单（对应前端所有 Agent 配置更新）。
 * 字段全部可选，仅更新非 null 字段。
 */
@Data
public class AgentUpdateForm {

    // ===== 基本信息 =====

    @Size(max = 100, message = "Agent名称不能超过100字")
    private String name;

    @Size(max = 500, message = "描述不能超过500字")
    private String description;

    private String avatar;

    private List<String> tags;

    // ===== AI 连接配置 =====

    private String aiBaseUrl;

    private String aiApiKey;

    private String aiModel;

    // ===== 模型配置 =====

    private String modelProvider;

    private String modelName;

    private BigDecimal temperature;

    private Integer maxTokens;

    private BigDecimal topP;

    // ===== 提示词配置 =====

    private String systemPrompt;

    /** 提示词变量 JSON 数组 */
    private List<Map<String, Object>> promptVariables;

    // ===== 记忆配置 =====

    /** 记忆策略：buffer/summary/vector/sliding_window/full */
    private String memoryStrategy;

    private Integer workingWindow;

    private Integer longTermEnabled;

    private List<Long> knowledgeBaseIds;

    // ===== Token 价格配置（美元/百万 token） =====

    private BigDecimal inputPricePerMillion;

    private BigDecimal cachedInputPricePerMillion;

    private BigDecimal outputPricePerMillion;

    // ===== 执行配置 =====

    private Integer maxIterations;

    /** 超时时间（ms） */
    private Integer timeout;

    private Integer reflectionEnabled;

    private Integer reflectionDepth;

    private Map<String, Object> outputSchema;
}
