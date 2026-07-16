package com.agentmanagement.service.impl;

import com.agentmanagement.common.BusinessException;
import com.agentmanagement.common.ResultCode;
import com.agentmanagement.entity.Budget;
import com.agentmanagement.entity.CostRecord;
import com.agentmanagement.form.BudgetForm;
import com.agentmanagement.mapper.BudgetMapper;
import com.agentmanagement.mapper.CostRecordMapper;
import com.agentmanagement.security.SecurityUtils;
import com.agentmanagement.service.BudgetService;
import com.agentmanagement.vo.BudgetVO;
import com.agentmanagement.vo.CostOverviewVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class BudgetServiceImpl extends ServiceImpl<BudgetMapper, Budget> implements BudgetService {

    @Autowired
    private BudgetMapper budgetMapper;

    @Autowired
    private CostRecordMapper costRecordMapper;

    @Override
    public List<BudgetVO> listBudgets() {
        Long workspaceId = SecurityUtils.currentWorkspaceId();
        LambdaQueryWrapper<Budget> qw = new LambdaQueryWrapper<Budget>();
        qw.eq(Budget::getWorkspaceId, workspaceId);
        qw.orderByDesc(Budget::getCreatedAt);

        List<Budget> budgets = budgetMapper.selectList(qw);
        List<BudgetVO> result = new ArrayList<BudgetVO>();
        for (Budget b : budgets) {
            result.add(toVO(b));
        }
        return result;
    }

    @Override
    public BudgetVO createBudget(BudgetForm form) {
        Long workspaceId = SecurityUtils.currentWorkspaceId();

        Budget entity = new Budget();
        entity.setWorkspaceId(workspaceId);
        entity.setName(form.getName());
        entity.setScope(form.getScope());
        entity.setScopeId(form.getScopeId());
        entity.setPeriod(form.getPeriod());
        entity.setLimitAmount(form.getLimitAmount());
        entity.setCurrentAmount(BigDecimal.ZERO);
        entity.setWarnPercent(form.getWarnPercent());
        // Boolean → Integer 转换
        entity.setMeltdownEnabled(form.getMeltdownEnabled() ? 1 : 0);
        entity.setNotifyChannels(form.getNotifyChannels() != null ? form.getNotifyChannels() : new ArrayList<String>());
        entity.setEnabled(form.getEnabled() ? 1 : 0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        budgetMapper.insert(entity);
        return toVO(budgetMapper.selectById(entity.getId()));
    }

    @Override
    public BudgetVO toggleBudget(Long id, boolean enabled) {
        Budget budget = requireBudgetInWorkspace(id);
        Budget update = new Budget();
        update.setId(budget.getId());
        update.setEnabled(enabled ? 1 : 0);
        update.setUpdatedAt(LocalDateTime.now());
        budgetMapper.updateById(update);
        return toVO(budgetMapper.selectById(id));
    }

    @Override
    public void deleteBudget(Long id) {
        Budget budget = requireBudgetInWorkspace(id);
        budgetMapper.deleteById(budget.getId());
    }

    @Override
    public CostOverviewVO getCostOverview(String period) {
        Long workspaceId = SecurityUtils.currentWorkspaceId();

        // 计算本月总花费
        LocalDate now = LocalDate.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = now.atTime(LocalTime.MAX);

        LambdaQueryWrapper<CostRecord> qw = new LambdaQueryWrapper<CostRecord>();
        qw.eq(CostRecord::getWorkspaceId, workspaceId);
        qw.between(CostRecord::getRecordedAt, monthStart, monthEnd);
        List<CostRecord> records = costRecordMapper.selectList(qw);

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal todayCost = BigDecimal.ZERO;
        LocalDateTime todayStart = now.atStartOfDay();

        for (CostRecord r : records) {
            totalCost = totalCost.add(r.getCost());
            if (r.getRecordedAt() != null && !r.getRecordedAt().isBefore(todayStart)) {
                todayCost = todayCost.add(r.getCost());
            }
        }

        // 取第一个启用的全局月预算作为预算上下文
        LambdaQueryWrapper<Budget> budgetQw = new LambdaQueryWrapper<Budget>();
        budgetQw.eq(Budget::getWorkspaceId, workspaceId);
        budgetQw.eq(Budget::getScope, "global");
        budgetQw.eq(Budget::getEnabled, 1);
        budgetQw.eq(Budget::getPeriod, "monthly");
        budgetQw.last("LIMIT 1");
        Budget budget = budgetMapper.selectOne(budgetQw);

        BigDecimal budgetLimit = budget != null ? budget.getLimitAmount() : new BigDecimal("2000");
        BigDecimal budgetRemaining = budgetLimit.subtract(totalCost);
        BigDecimal budgetPercent = totalCost.multiply(new BigDecimal("100"))
                .divide(budgetLimit, 1, RoundingMode.HALF_UP);

        CostOverviewVO vo = new CostOverviewVO();
        vo.setTotalCost(totalCost);
        vo.setBudgetLimit(budgetLimit);
        vo.setBudgetRemaining(budgetRemaining);
        vo.setBudgetPercent(budgetPercent);
        vo.setTodayCost(todayCost);
        vo.setYesterdayCost(BigDecimal.ZERO);
        vo.setProjectedMonthCost(totalCost);
        vo.setMeltdownStatus("normal");
        return vo;
    }

    // ==================== 私有辅助 ====================

    private Budget requireBudgetInWorkspace(Long id) {
        Budget budget = budgetMapper.selectById(id);
        Long workspaceId = SecurityUtils.currentWorkspaceId();
        if (budget == null || !workspaceId.equals(budget.getWorkspaceId())) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        return budget;
    }

    /** Budget entity → BudgetVO（Integer 0/1 → Boolean，limitAmount → limit） */
    private BudgetVO toVO(Budget b) {
        BudgetVO vo = new BudgetVO();
        vo.setId(b.getId());
        vo.setName(b.getName());
        vo.setScope(b.getScope());
        vo.setScopeId(b.getScopeId());
        vo.setPeriod(b.getPeriod());
        vo.setLimit(b.getLimitAmount());
        vo.setCurrentAmount(b.getCurrentAmount());
        vo.setWarnPercent(b.getWarnPercent());
        vo.setMeltdownEnabled(b.getMeltdownEnabled() != null && b.getMeltdownEnabled() != 0);
        vo.setNotifyChannels(b.getNotifyChannels());
        vo.setEnabled(b.getEnabled() != null && b.getEnabled() != 0);
        vo.setCreatedAt(b.getCreatedAt());
        vo.setUpdatedAt(b.getUpdatedAt());
        return vo;
    }
}
