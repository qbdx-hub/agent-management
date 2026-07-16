package com.agentmanagement.security;

import com.agentmanagement.common.BusinessException;
import com.agentmanagement.common.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 安全上下文工具：从 SecurityContextHolder 取当前登录用户 id、从 X-Workspace-Id 头取当前工作空间 id。
 * JwtAuthenticationFilter 解析 token 成功后，把 userId（Long）作为 principal 放入上下文。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 取当前登录用户 id；未登录（上下文为空或 principal 非 Long）抛 UNAUTHORIZED。
     */
    public static Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }

    /**
     * 取当前工作空间 id（来自 X-Workspace-Id 请求头，前端 axios 拦截器每次请求都会带）。
     * 缺失或格式非法抛 WORKSPACE_REQUIRED。
     */
    public static Long currentWorkspaceId() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new BusinessException(ResultCode.WORKSPACE_REQUIRED);
        }
        HttpServletRequest request = attrs.getRequest();
        String header = request.getHeader("X-Workspace-Id");
        if (!StringUtils.hasText(header)) {
            throw new BusinessException(ResultCode.WORKSPACE_REQUIRED);
        }
        try {
            return Long.valueOf(header.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.WORKSPACE_REQUIRED);
        }
    }

    /**
     * 取当前登录用户 id；未登录返回 null（不抛异常）。
     * 供审计切面在 login/register 等无认证上下文的场景使用。
     */
    public static Long currentUserIdSafe() {
        try {
            return currentUserId();
        } catch (BusinessException e) {
            return null;
        }
    }

    /**
     * 取当前工作空间 id；请求头缺失返回 null（不抛异常）。供审计切面使用。
     */
    public static Long currentWorkspaceIdSafe() {
        try {
            return currentWorkspaceId();
        } catch (BusinessException e) {
            return null;
        }
    }
}
