package com.agentmanagement.service;

import com.agentmanagement.entity.Budget;
import com.agentmanagement.form.BudgetForm;
import com.agentmanagement.vo.BudgetVO;
import com.agentmanagement.vo.CostOverviewVO;
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
}
