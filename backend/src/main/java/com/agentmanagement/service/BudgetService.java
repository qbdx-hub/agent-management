package com.agentmanagement.service;

import com.agentmanagement.entity.Budget;
import com.agentmanagement.form.BudgetForm;
import com.agentmanagement.vo.BudgetVO;
import com.agentmanagement.vo.CostBreakdownVO;
import com.agentmanagement.vo.CostOverviewVO;
import com.agentmanagement.vo.CostTrendVO;
import com.agentmanagement.vo.CostRecordVO;
import com.agentmanagement.common.PageResult;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface BudgetService extends IService<Budget> {

    /** 获取当前工作空间的预算列表 */
    List<BudgetVO> listBudgets();

    /** 创建预算 */
    BudgetVO createBudget(BudgetForm form);

    /** 启用/禁用预算 */
    BudgetVO toggleBudget(Long id, boolean enabled);

    /** 删除预算 */
    void deleteBudget(Long id);

    /** 成本概览 */
    CostOverviewVO getCostOverview(String period);

    /** 成本明细（按 model/agent/member 分组） */
    List<CostBreakdownVO> getCostBreakdown(String dimension, String period);

    /** 成本趋势（按天/小时） */
    List<CostTrendVO> getCostTrend(String period, String granularity);

    /** 费用记录分页列表 */
    PageResult<CostRecordVO> getCostRecords(int page, int pageSize);

    /**
     * 调用前预算检查：命中启用熔断且已超支的预算时抛 BUDGET_EXCEEDED。
     * 匹配范围：global 全部生效；user/agent 按本次调用方与 Agent 匹配。
     */
    void assertBudgetAllowed(Long workspaceId, Long userId, Long agentId);

    /** 调用成功后扣减匹配预算的已用金额（current_amount 累加，展示层按 cost_record 实时计算） */
    void deductAfterCall(Long workspaceId, Long userId, Long agentId, java.math.BigDecimal cost);
}
