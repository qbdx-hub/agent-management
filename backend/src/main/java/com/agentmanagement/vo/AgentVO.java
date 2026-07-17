package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Agent 详情 VO（对应前端 AgentDetail）。
 * 数据库 agent 表是扁平结构，此处组装为前端期望的嵌套 config + stats。
 * <p>字段名映射注意（entity ↔ 前端）：
 * <ul>
 *   <li>memoryStrategy ↔ memory.shortTermStrategy</li>
 *   <li>maxIterations ↔ execution.maxSteps</li>
 *   <li>timeout(ms) ↔ execution.timeoutSeconds(秒)</li>
 * </ul>
 */
@Data
public class AgentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String description;

    private String avatar;

    private String status;

    private List<String> tags;

    private Long createdBy;

    private String creatorName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** AI 连接配置（脱敏：apiKey 只返回前 8 位 + ***） */
    private String aiBaseUrl;
    private String aiApiKeyMasked;
    private String aiModel;

    /** Token 价格配置（美元/百万 token） */
    private BigDecimal inputPricePerMillion;
    private BigDecimal cachedInputPricePerMillion;
    private BigDecimal outputPricePerMillion;

    private Config config;

    private Stats stats;

    @Data
    public static class Config {
        private String modelProvider;
        private String modelName;
        private BigDecimal temperature;
        private Integer maxTokens;
        private BigDecimal topP;
        private String systemPrompt;
        /** 提示词变量（每项 {name,label,type,options,defaultValue,required}） */
        private List<Map<String, Object>> promptVariables;
        private List<BoundTool> boundTools;
        private Memory memory;
        private Execution execution;
    }

    @Data
    public static class BoundTool {
        private Long toolId;
        private String toolName;
        private String toolIcon;
        private Boolean enabled;
    }

    @Data
    public static class Memory {
        private Integer workingWindow;
        /** 对应 entity.memoryStrategy */
        private String shortTermStrategy;
        private Boolean longTermEnabled;
        private List<Long> knowledgeBaseIds;
    }

    @Data
    public static class Execution {
        /** 对应 entity.maxIterations */
        private Integer maxSteps;
        /** 对应 entity.timeout(ms) 折算的秒 */
        private Integer timeoutSeconds;
        private Boolean reflectionEnabled;
        private Integer reflectionDepth;
        private Map<String, Object> outputSchema;
    }

    @Data
    public static class Stats {
        private Long totalSessions;
        private Long totalMessages;
        private Long totalTokens;
        private BigDecimal totalCost;
        private BigDecimal successRate;
        private Integer avgLatencyMs;
        private BigDecimal avgStepsPerSession;
    }
}
