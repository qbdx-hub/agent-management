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
 * 工具表实体（对应 tool 表）。
 * 含端点配置、参数与响应、认证、重试、MCP 配置、统计字段。
 * headers/parameters/authConfig/mcpArgs/mcpEnvVars 为 JSON 列，用 JacksonTypeHandler；需 autoResultMap = true。
 */
@Data
@TableName(value = "tool", autoResultMap = true)
public class Tool implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long workspaceId;

    /** 工具标识名 */
    private String name;

    private String displayName;

    private String description;

    /** 分类：search/compute/operate/perceive/notify/custom */
    private String category;

    private String icon;

    /** 版本号 */
    private String version;

    /** 类型：api/mcp/builtin */
    private String type;

    /** 状态：active/inactive/error */
    private String status;

    // ============ 端点配置 ============

    private String endpointUrl;

    /** HTTP 方法 */
    private String method;

    /** 请求头 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> headers;

    /** 端点超时时间（ms） */
    private Integer endpointTimeout;

    // ============ 参数与响应 ============

    /** 参数定义 JSON 数组 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> parameters;

    /** 响应映射表达式 */
    private String responseMapping;

    // ============ 认证 ============

    /** 凭证引用标识 */
    private String credentialRef;

    /** 认证类型：none/api_key/bearer/oauth */
    private String authType;

    /** 认证配置 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> authConfig;

    // ============ 重试 ============

    /** 失败是否重试：0-否 1-是 */
    private Integer retryOnFail;

    private Integer maxRetries;

    // ============ MCP 配置 ============

    private String mcpServerName;

    /** MCP 传输方式：stdio/sse */
    private String mcpTransport;

    /** MCP 启动命令 */
    private String mcpCommand;

    /** MCP 命令参数 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> mcpArgs;

    /** MCP 环境变量 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> mcpEnvVars;

    // ============ 统计 ============

    /** 绑定 Agent 数 */
    private Integer bindAgentCount;

    /** 总调用次数 */
    private Long totalCalls;

    /** 成功率（%） */
    private BigDecimal successRate;

    /** 平均延迟（ms） */
    private Integer avgLatencyMs;

    /** P99 延迟（ms） */
    private Integer p99LatencyMs;

    // ============ 审计 ============

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
