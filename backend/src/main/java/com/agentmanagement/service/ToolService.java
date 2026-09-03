package com.agentmanagement.service;

import com.agentmanagement.common.PageResult;
import com.agentmanagement.entity.Tool;
import com.agentmanagement.form.ToolQueryForm;
import com.agentmanagement.form.ToolRegisterForm;
import com.agentmanagement.form.ToolUpdateForm;
import com.agentmanagement.vo.ToolStatsVO;
import com.agentmanagement.vo.ToolSummaryVO;
import com.agentmanagement.vo.ToolTestResultVO;
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

    /** 工具连通性测试：真实发起 HTTP 调用并落 tool_call_record、回填统计 */
    ToolTestResultVO testTool(Long id, java.util.Map<String, Object> parameters);

    /**
     * 连通性测试（显式工作空间版）—— 供异步线程调用（工作流执行引擎、会话工具调用循环）。
     * 这些线程没有 SecurityContext（SecurityUtils 会抛异常），workspaceId 由调用方从请求线程捕获后显式传入；
     * agentId/sessionId 仅用于 tool_call_record 归因，可为 null。
     */
    ToolTestResultVO testTool(Long id, java.util.Map<String, Object> parameters,
                              Long workspaceId, Long agentId, Long sessionId);

    /** 工具调用统计：由 tool_call_record 实时聚合 */
    ToolStatsVO getToolStats(Long id);

    /** 回填工具调用统计（供内置工具执行器复用，与 testTool 落库后同一口径） */
    void refreshStatsAfterCall(Long toolId, boolean success);
}
