package com.agentmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * API 密钥表实体（对应 api_key 表）。
 * 只存 SHA-256 摘要与展示掩码，明文仅在创建响应中返回一次。
 */
@Data
@TableName("api_key")
public class ApiKey implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属用户 ID */
    private Long userId;

    private String name;

    /** SHA-256 摘要（hex），用于校验，不可逆 */
    private String keyHash;

    /** 展示掩码，如 sk-my-****-****-8f2a */
    private String mask;

    /** 状态：active/disabled */
    private String status;

    private LocalDateTime lastUsedAt;

    private LocalDateTime createdAt;
}
