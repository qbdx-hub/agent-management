package com.agentmanagement.mapper;

import com.agentmanagement.entity.Approval;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批记录表 Mapper。
 */
@Mapper
public interface ApprovalMapper extends BaseMapper<Approval> {
}
