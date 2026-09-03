package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 角色 VO（角色权限页卡片）。
 */
@Data
public class RoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String label;

    private String description;

    private Boolean isSystem;

    private Long memberCount;

    private List<String> permissions;
}
