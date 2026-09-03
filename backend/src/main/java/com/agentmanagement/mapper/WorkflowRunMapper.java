package com.agentmanagement.mapper;

import com.agentmanagement.entity.WorkflowRun;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流运行记录表 Mapper。
 */
@Mapper
public interface WorkflowRunMapper extends BaseMapper<WorkflowRun> {
}
