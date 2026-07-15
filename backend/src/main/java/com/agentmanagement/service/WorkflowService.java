package com.agentmanagement.service;

import com.agentmanagement.common.PageResult;
import com.agentmanagement.entity.Workflow;
import com.agentmanagement.form.WorkflowCreateForm;
import com.agentmanagement.form.WorkflowQueryForm;
import com.agentmanagement.form.WorkflowSaveForm;
import com.agentmanagement.vo.WorkflowSummaryVO;
import com.agentmanagement.vo.WorkflowVO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface WorkflowService extends IService<Workflow> {

    /** 分页列表（按当前工作空间隔离） */
    PageResult<WorkflowSummaryVO> pageWorkflows(WorkflowQueryForm form);

    /** 详情（含 nodes / edges） */
    WorkflowVO getWorkflowDetail(Long id);

    /** 创建空工作流（draft） */
    WorkflowVO createWorkflow(WorkflowCreateForm form);

    /** 保存画布（全量替换 nodes / edges，落 position_x/y） */
    WorkflowVO saveWorkflow(Long id, WorkflowSaveForm form);

    /** 删除（级联清理 nodes / edges） */
    void removeWorkflow(Long id);
}
