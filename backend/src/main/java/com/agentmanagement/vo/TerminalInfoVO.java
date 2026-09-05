package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * GET /terminal/info —— 终端环境信息（移动端显示提示符/快捷命令/角色用）。
 */
@Data
public class TerminalInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 服务器操作系统名（如 Windows 11 / Linux），前端据此切换 dir/ls 快捷命令 */
    private String os;

    /** 当前用户在该空间的成员角色（owner/admin/member） */
    private String role;

    /** 沙箱目录显示名（ws-{workspaceId}） */
    private String sandboxPath;

    /** 空间是否对成员开放终端（owner/admin 不受此开关限制） */
    private Boolean memberTerminalEnabled;
}
