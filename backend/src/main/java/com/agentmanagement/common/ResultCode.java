package com.agentmanagement.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务错误码统一定义。
 * 0 = 成功；1xxx 通用错误；2xxx 业务错误（后续模块扩展）。
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(0, "success"),

    // ===== 通用错误 1xxx =====
    PARAM_ERROR(1001, "参数错误"),
    /** 未登录 / token 失效：由 RestAuthenticationEntryPoint 返回，HTTP 401 */
    UNAUTHORIZED(1002, "未登录或token已过期"),
    FORBIDDEN(1003, "无操作权限"),
    /** 登录失败（用户名不存在或密码错误，文案统一以防用户名枚举） */
    LOGIN_FAILED(1004, "用户名或密码错误"),
    ACCOUNT_DISABLED(1005, "账号已被禁用"),
    /** 用户名已被注册 */
    USERNAME_EXISTS(1006, "用户名已被注册"),
    /** 邮箱已被注册 */
    EMAIL_EXISTS(1007, "邮箱已被注册"),
    /** 缺少工作空间上下文（前端未带 X-Workspace-Id 头） */
    WORKSPACE_REQUIRED(1008, "缺少工作空间上下文"),

    // ===== 业务错误 2xxx（后续模块扩展） =====
    DATA_NOT_FOUND(2001, "数据不存在"),
    /** Agent 不存在或不属于当前工作空间 */
    AGENT_NOT_FOUND(2002, "Agent不存在"),
    /** 工具不存在或不属于当前工作空间 */
    TOOL_NOT_FOUND(2003, "工具不存在"),
    /** Agent 状态值非法（不在 draft/testing/published/paused/archived 范围内） */
    AGENT_STATUS_INVALID(2004, "Agent状态非法");

    private final Integer code;
    private final String message;
}
