package com.agentmanagement.service;

import com.agentmanagement.common.PageResult;
import com.agentmanagement.entity.Agent;
import com.agentmanagement.form.AgentCreateForm;
import com.agentmanagement.form.AgentQueryForm;
import com.agentmanagement.form.AgentUpdateForm;
import com.agentmanagement.vo.AgentSummaryVO;
import com.agentmanagement.vo.AgentVO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AgentService extends IService<Agent> {

    /** 分页列表（按当前工作空间隔离） */
    PageResult<AgentSummaryVO> pageAgents(AgentQueryForm form);

    /** 详情（扁平 entity 组装为嵌套 config + stats） */
    AgentVO getAgentDetail(Long id);

    /** 新建（落 workspaceId / createdBy / 模型配置） */
    AgentVO createAgent(AgentCreateForm form);

    /** 编辑基本信息 */
    void updateAgent(Long id, AgentUpdateForm form);

    /** 修改状态 */
    void updateAgentStatus(Long id, String status);

    /** 删除（校验工作空间归属后物理删除） */
    void removeAgent(Long id);
}
