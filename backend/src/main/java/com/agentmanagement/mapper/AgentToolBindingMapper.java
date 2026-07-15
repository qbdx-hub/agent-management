package com.agentmanagement.mapper;

import com.agentmanagement.entity.AgentToolBinding;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * Agent 工具绑定 Mapper（靠启动类 @MapperScan 扫描，不加 @Mapper）。
 * 用于删除 Agent 时级联清理绑定关系（DB 无 ON DELETE CASCADE）。
 */
public interface AgentToolBindingMapper extends BaseMapper<AgentToolBinding> {
}
