package com.agentmanagement.service;

import com.agentmanagement.common.PageResult;
import com.agentmanagement.entity.Tool;
import com.agentmanagement.form.ToolQueryForm;
import com.agentmanagement.form.ToolRegisterForm;
import com.agentmanagement.form.ToolUpdateForm;
import com.agentmanagement.vo.ToolSummaryVO;
import com.agentmanagement.vo.ToolVO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface ToolService extends IService<Tool> {

    /** 分页列表（按当前工作空间隔离） */
    PageResult<ToolSummaryVO> pageTools(ToolQueryForm form);

    /** 详情 */
    ToolVO getToolDetail(Long id);

    /** 注册工具（落 workspaceId / createdBy） */
    ToolVO registerTool(ToolRegisterForm form);

    /** 编辑工具 */
    void updateTool(Long id, ToolUpdateForm form);

    /** 删除 */
    void removeTool(Long id);
}
