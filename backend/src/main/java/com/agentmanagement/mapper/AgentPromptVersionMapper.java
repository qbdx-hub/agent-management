package com.agentmanagement.mapper;

import com.agentmanagement.entity.AgentPromptVersion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * Agent 提示词版本 Mapper（靠启动类 @MapperScan 扫描，不加 @Mapper）。
 * 用于删除 Agent 时级联清理历史版本（DB 无 ON DELETE CASCADE）。
 */
public interface AgentPromptVersionMapper extends BaseMapper<AgentPromptVersion> {
}
