package com.agentmanagement.mapper;

import com.agentmanagement.entity.AuditLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 审计日志 Mapper（靠启动类 @MapperScan 扫描，不加 @Mapper）。
 */
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
