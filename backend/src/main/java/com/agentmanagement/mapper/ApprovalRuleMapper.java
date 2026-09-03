package com.agentmanagement.mapper;

import com.agentmanagement.entity.ApprovalRule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批规则表 Mapper。
 */
@Mapper
public interface ApprovalRuleMapper extends BaseMapper<ApprovalRule> {
}
