package com.agentmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作空间表实体（对应 workspace 表）。
 * 登录只用 id + name，其余字段照建以备后续模块复用。
 */
@Data
@TableName("workspace")
public class Workspace implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String icon;

    private Long ownerId;

    private Integer maxAgents;

    private Integer maxMembers;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    private String defaultModelProvider;

    private Integer sessionRetentionDays;

    private Integer autoArchiveDays;

    private Long maxTokensPerTask;

    private String language;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
