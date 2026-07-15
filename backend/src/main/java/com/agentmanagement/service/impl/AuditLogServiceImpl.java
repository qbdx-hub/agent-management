package com.agentmanagement.service.impl;

import com.agentmanagement.entity.AuditLog;
import com.agentmanagement.mapper.AuditLogMapper;
import com.agentmanagement.service.AuditLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志服务实现。
 */
@Slf4j
@Service
public class AuditLogServiceImpl extends ServiceImpl<AuditLogMapper, AuditLog> implements AuditLogService {

    @Override
    public void log(Long workspaceId, Long userId, String userName,
                    String action, String actionLabel,
                    String resourceType, Long resourceId, String resourceName,
                    String detail, String result,
                    String ipAddress, String userAgent) {
        AuditLog auditLog = new AuditLog();
        auditLog.setWorkspaceId(workspaceId);
        auditLog.setUserId(userId);
        auditLog.setUserName(userName);
        auditLog.setAction(action);
        auditLog.setActionLabel(actionLabel);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setResourceName(resourceName);
        auditLog.setDetail(detail);
        auditLog.setResult(result);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);
        auditLog.setCreatedAt(LocalDateTime.now());

        try {
            baseMapper.insert(auditLog);
        } catch (Exception e) {
            // 审计日志写入失败不应阻断业务
            log.error("审计日志写入失败: action={}, resource={}", action, resourceName, e);
        }
    }

    @Override
    public List<AuditLog> listByWorkspace(Long workspaceId, int limit) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<AuditLog>()
                        .eq(AuditLog::getWorkspaceId, workspaceId)
                        .orderByDesc(AuditLog::getCreatedAt)
                        .last("LIMIT " + limit));
    }
}
