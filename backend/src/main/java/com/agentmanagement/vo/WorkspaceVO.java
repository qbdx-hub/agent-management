package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作空间条目 VO（顶栏切换列表 / 创建返回）。
 */
@Data
public class WorkspaceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String description;

    /** 当前用户在该空间的角色 */
    private String role;

    private Long memberCount;

    private Long agentCount;

    private LocalDateTime createdAt;
}
