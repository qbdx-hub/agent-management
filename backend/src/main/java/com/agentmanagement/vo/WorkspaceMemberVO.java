package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作空间成员 VO（成员管理页表格行）。
 * agentCount / sessionCount30d 由 agent.created_by 与 session.created_by 实时统计。
 */
@Data
public class WorkspaceMemberVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String nickname;

    private String avatar;

    private String email;

    /** 空间内角色：owner/admin/member（原样返回，前端做映射） */
    private String role;

    private String roleLabel;

    private LocalDateTime joinedAt;

    private LocalDateTime lastActiveAt;

    private Long agentCount;

    private Long sessionCount30d;
}
