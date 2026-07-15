package com.agentmanagement.service;

import com.agentmanagement.entity.AuditLog;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 审计日志服务。
 */
public interface AuditLogService extends IService<AuditLog> {

    /**
     * 记录审计日志。
     *
     * @param workspaceId    工作空间 ID
     * @param userId         操作者 ID
     * @param userName       操作者用户名
     * @param action         操作类型（如 knowledge.create）
     * @param actionLabel    操作显示名（如 创建知识库）
     * @param resourceType   资源类型（如 knowledge_base）
     * @param resourceId     资源 ID
     * @param resourceName   资源名称
     * @param detail         操作详情
     * @param result         结果（success/failure）
     * @param ipAddress      IP 地址
     * @param userAgent      User-Agent
     */
    void log(Long workspaceId, Long userId, String userName,
             String action, String actionLabel,
             String resourceType, Long resourceId, String resourceName,
             String detail, String result,
             String ipAddress, String userAgent);

    /**
     * 查询工作空间下的审计日志列表（按时间倒序）。
     *
     * @param workspaceId 工作空间 ID
     * @param limit       返回条数限制
     * @return 日志列表
     */
    List<AuditLog> listByWorkspace(Long workspaceId, int limit);
}
