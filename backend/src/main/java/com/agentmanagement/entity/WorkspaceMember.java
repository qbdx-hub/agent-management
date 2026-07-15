package com.agentmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作空间成员表（对应 workspace_member 表）。
 */
@Data
@TableName("workspace_member")
public class WorkspaceMember implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long workspaceId;

    private Long userId;

    /** 工作空间内角色：owner / admin / member */
    private String role;

    private LocalDateTime joinedAt;

    private LocalDateTime lastActiveAt;
}
