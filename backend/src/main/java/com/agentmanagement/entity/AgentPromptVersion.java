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
 * Agent 提示词版本表实体（对应 agent_prompt_version 表）。
 * 用于提示词的历史版本管理。
 * promptVariables 为 JSON 列，用 JacksonTypeHandler；需 autoResultMap = true。
 */
@Data
@TableName(value = "agent_prompt_version", autoResultMap = true)
public class AgentPromptVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long agentId;

    /** 版本号 */
    private String versionNumber;

    private String systemPrompt;

    /** 变量定义 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> promptVariables;

    /** 变更说明 */
    private String changeNote;

    /** 创建者 ID */
    private Long changedBy;

    private LocalDateTime createdAt;
}
