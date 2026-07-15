package com.agentmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计日志表实体（对应 audit_log 表）。
 * 记录用户对资源的关键操作（增删改/发布/注册等），用于安全审计与追溯。
 */
@Data
@TableName("audit_log")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long workspaceId;

    private Long userId;

    /** 操作者用户名（冗余） */
    private String userName;

    /** 操作类型：agent.create/agent.update/agent.delete/tool.register... */
    private String action;

    /** 操作显示名 */
    private String actionLabel;

    /** 资源类型：agent/tool/session/workspace/member */
    private String resourceType;

    private Long resourceId;

    private String resourceName;

    /** 详情 */
    private String detail;

    /** 结果：success/failure */
    private String result;

    private String ipAddress;

    private String userAgent;

    private LocalDateTime createdAt;
}
