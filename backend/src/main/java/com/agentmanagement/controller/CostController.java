package com.agentmanagement.controller;

import com.agentmanagement.common.Result;
import com.agentmanagement.form.BudgetForm;
import com.agentmanagement.service.BudgetService;
import com.agentmanagement.vo.BudgetVO;
import com.agentmanagement.vo.CostOverviewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 成本管理 RESTful 接口。
 * 前缀 /api/v1 由 context-path 统一加，类上不再写。
 * 对齐前端 api/cost.ts：
 *   GET  /cost/budgets         — 预算列表
 *   POST /cost/budgets         — 创建预算
 *   PUT  /cost/budgets/{id}/status?enabled=0/1 — 启用/禁用
 *   DELETE /cost/budgets/{id}  — 删除预算
 *   GET  /cost/overview        — 成本概览
 */
@RestController
@RequestMapping("/cost")
public class CostController {

    @Autowired
    private BudgetService budgetService;

    /** GET /cost/budgets —— 预算列表 */
    @GetMapping("/budgets")
    public Result<List<BudgetVO>> listBudgets() {
        return Result.success(budgetService.listBudgets());
    }

    /** POST /cost/budgets —— 创建预算 */
    @PostMapping("/budgets")
    public Result<BudgetVO> createBudget(@Valid @RequestBody BudgetForm form) {
        return Result.success(budgetService.createBudget(form));
    }

    /** PUT /cost/budgets/{id}/status?enabled=0|1 —— 启用/禁用预算 */
    @PutMapping("/budgets/{id}/status")
    public Result<BudgetVO> updateBudgetStatus(@PathVariable("id") Long id,
                                                @RequestParam("enabled") Integer enabled) {
        return Result.success(budgetService.toggleBudget(id, enabled != 0));
    }

    /** DELETE /cost/budgets/{id} —— 删除预算 */
    @DeleteMapping("/budgets/{id}")
    public Result<Void> deleteBudget(@PathVariable("id") Long id) {
        budgetService.deleteBudget(id);
        return Result.success();
    }

    /** GET /cost/overview —— 成本概览 */
    @GetMapping("/overview")
    public Result<CostOverviewVO> getOverview(@RequestParam(value = "period", defaultValue = "this_month") String period) {
        return Result.success(budgetService.getCostOverview(period));
    }
}
