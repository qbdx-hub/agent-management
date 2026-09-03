package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * API 密钥视图对象。
 * 列表/详情只含 mask（掩码）；仅创建接口回填 key（明文，仅此一次）。
 */
@Data
public class ApiKeyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    /** 展示掩码，如 sk-my-****-****-8f2a */
    private String mask;

    /** 状态：active/disabled */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime lastUsedAt;

    /** 完整密钥明文 —— 仅 POST 创建响应返回一次，其余场景恒为 null */
    private String key;
}
