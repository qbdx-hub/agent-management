package com.agentmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * API Key 表实体（对应 api_key 表）。
 * 第三方模型/服务密钥的存储记录，仅存前缀与哈希，明文不入库。
 * scopes 为 JSON 列，用 JacksonTypeHandler；需 autoResultMap = true。
 */
@Data
@TableName(value = "api_key", autoResultMap = true)
public class ApiKey implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long workspaceId;

    /** 提供商：openai/anthropic/... */
    private String provider;

    private String name;

    /** Key 前缀（用于显示） */
    private String keyPrefix;

    /** Key 哈希值 */
    private String keyHash;

    /** 是否默认 Key：0-否 1-是 */
    private Integer isDefault;

    /** 权限范围 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> scopes;

    /** 状态：active/revoked */
    private String status;

    private LocalDateTime lastUsedAt;

    private LocalDateTime expiresAt;

    private Long createdBy;

    private LocalDateTime createdAt;
}
