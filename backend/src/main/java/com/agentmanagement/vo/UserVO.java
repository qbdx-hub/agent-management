package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录响应里的 user 对象（不含 password）。
 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String email;

    /** 系统角色（大写）：ADMIN / DEVELOPER / VIEWER */
    private String role;

    private List<String> permissions;

    private List<WorkspaceBriefVO> workspaces;
}
