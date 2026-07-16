package com.agentmanagement.controller;

import com.agentmanagement.common.PageResult;
import com.agentmanagement.common.Result;
import com.agentmanagement.form.BudgetForm;
import com.agentmanagement.service.BudgetService;
import com.agentmanagement.vo.BudgetVO;
import com.agentmanagement.vo.CostBreakdownVO;
import com.agentmanagement.vo.CostOverviewVO;
import com.agentmanagement.vo.CostRecordVO;
import com.agentmanagement.vo.CostTrendVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 成本管理 RESTful 接口。
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

    /** GET /cost/breakdown —— 成本明细（按 model/agent/member 分组） */
    @GetMapping("/breakdown")
    public Result<List<CostBreakdownVO>> getBreakdown(
            @RequestParam(value = "dimension", defaultValue = "model") String dimension,
            @RequestParam(value = "period", defaultValue = "this_month") String period) {
        return Result.success(budgetService.getCostBreakdown(dimension, period));
    }

    /** GET /cost/trend —— 成本趋势 */
    @GetMapping("/trend")
    public Result<List<CostTrendVO>> getTrend(
            @RequestParam(value = "period", defaultValue = "30d") String period,
            @RequestParam(value = "granularity", defaultValue = "day") String granularity) {
        return Result.success(budgetService.getCostTrend(period, granularity));
    }

    /** GET /cost/records —— 费用记录分页列表 */
    @GetMapping("/records")
    public Result<PageResult<CostRecordVO>> getRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(budgetService.getCostRecords(page, pageSize));
    }
}
