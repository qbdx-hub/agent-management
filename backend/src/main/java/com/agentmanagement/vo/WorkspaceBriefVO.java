package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录响应里 workspaces[] 的单项（工作空间概要）。
 */
@Data
public class WorkspaceBriefVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    /** 工作空间内角色：owner / admin / member（返回 DB 原值） */
    private String role;
}
