package com.agentmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动日志表实体（对应 activity_log 表）。
 * 工作空间维度的用户活动流（如 agent.created/tool.create/member.invite/session.start）。
 */
@Data
@TableName("activity_log")
public class ActivityLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long workspaceId;

    private Long userId;

    /** 用户名（冗余） */
    private String userName;

    /** 类型：agent.created/tool.create/member.invite/session.start... */
    private String type;

    private String description;

    /** 关联资源 ID */
    private Long relatedId;

    /** 关联资源类型 */
    private String relatedType;

    private LocalDateTime createdAt;
}
