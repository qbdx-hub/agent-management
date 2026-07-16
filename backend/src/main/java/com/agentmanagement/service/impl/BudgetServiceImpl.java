package com.agentmanagement.service.impl;

import com.agentmanagement.common.BusinessException;
import com.agentmanagement.common.PageResult;
import com.agentmanagement.common.ResultCode;
import com.agentmanagement.entity.Budget;
import com.agentmanagement.entity.CostRecord;
import com.agentmanagement.form.BudgetForm;
import com.agentmanagement.mapper.BudgetMapper;
import com.agentmanagement.mapper.CostRecordMapper;
import com.agentmanagement.security.SecurityUtils;
import com.agentmanagement.service.BudgetService;
import com.agentmanagement.vo.BudgetVO;
import com.agentmanagement.vo.CostBreakdownVO;
import com.agentmanagement.vo.CostOverviewVO;
import com.agentmanagement.vo.CostRecordVO;
import com.agentmanagement.vo.CostTrendVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        LocalDate now = LocalDate.now();

        // 时间范围
        LocalDateTime rangeStart, rangeEnd;
        if ("last_month".equals(period)) {
            LocalDate lastMonth = now.minusMonths(1);
            rangeStart = lastMonth.withDayOfMonth(1).atStartOfDay();
            rangeEnd = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()).atTime(LocalTime.MAX);
        } else {
            rangeStart = now.withDayOfMonth(1).atStartOfDay();
            rangeEnd = now.atTime(LocalTime.MAX);
        }

        LambdaQueryWrapper<CostRecord> qw = new LambdaQueryWrapper<CostRecord>();
        qw.eq(CostRecord::getWorkspaceId, workspaceId);
        qw.between(CostRecord::getRecordedAt, rangeStart, rangeEnd);
        List<CostRecord> records = costRecordMapper.selectList(qw);

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal todayCost = BigDecimal.ZERO;
        BigDecimal yesterdayCost = BigDecimal.ZERO;
        LocalDateTime todayStart = now.atStartOfDay();
        LocalDateTime yesterdayStart = now.minusDays(1).atStartOfDay();

        for (CostRecord r : records) {
            totalCost = totalCost.add(r.getCost());
            if (r.getRecordedAt() != null) {
                if (!r.getRecordedAt().isBefore(todayStart)) {
                    todayCost = todayCost.add(r.getCost());
                } else if (!r.getRecordedAt().isBefore(yesterdayStart)) {
                    yesterdayCost = yesterdayCost.add(r.getCost());
                }
            }
        }

        // 预算
        LambdaQueryWrapper<Budget> budgetQw = new LambdaQueryWrapper<Budget>();
        budgetQw.eq(Budget::getWorkspaceId, workspaceId);
        budgetQw.eq(Budget::getScope, "global");
        budgetQw.eq(Budget::getEnabled, 1);
        budgetQw.eq(Budget::getPeriod, "monthly");
        budgetQw.last("LIMIT 1");
        Budget budget = budgetMapper.selectOne(budgetQw);

        BigDecimal budgetLimit = budget != null ? budget.getLimitAmount() : new BigDecimal("2000");
        BigDecimal budgetRemaining = budgetLimit.subtract(totalCost);
        BigDecimal budgetPercent = BigDecimal.ZERO;
        if (budgetLimit.compareTo(BigDecimal.ZERO) > 0) {
            budgetPercent = totalCost.multiply(new BigDecimal("100"))
                    .divide(budgetLimit, 1, RoundingMode.HALF_UP);
        }

        // 预估本月费用（按天均摊）
        int dayOfMonth = now.getDayOfMonth();
        int daysInMonth = now.lengthOfMonth();
        BigDecimal projectedMonthCost = dayOfMonth > 0
                ? totalCost.multiply(new BigDecimal(daysInMonth)).divide(new BigDecimal(dayOfMonth), 2, RoundingMode.HALF_UP)
                : totalCost;

        // 熔断状态
        String meltdownStatus = "normal";
        if (budgetPercent.compareTo(new BigDecimal("90")) >= 0) {
            meltdownStatus = "meltdown";
        } else if (budgetPercent.compareTo(new BigDecimal("80")) >= 0) {
            meltdownStatus = "warning";
        }

        CostOverviewVO vo = new CostOverviewVO();
        vo.setTotalCost(totalCost);
        vo.setBudgetLimit(budgetLimit);
        vo.setBudgetRemaining(budgetRemaining);
        vo.setBudgetPercent(budgetPercent);
        vo.setTodayCost(todayCost);
        vo.setYesterdayCost(yesterdayCost);
        vo.setProjectedMonthCost(projectedMonthCost);
        vo.setMeltdownStatus(meltdownStatus);
        return vo;
    }

    @Override
    public List<CostBreakdownVO> getCostBreakdown(String dimension, String period) {
        Long workspaceId = SecurityUtils.currentWorkspaceId();
        LocalDate now = LocalDate.now();
        LocalDateTime rangeStart = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime rangeEnd = now.atTime(LocalTime.MAX);

        // 按维度聚合
        String groupField;
        if ("agent".equals(dimension)) {
            groupField = "agent_name";
        } else if ("member".equals(dimension)) {
            groupField = "user_name";
        } else {
            groupField = "model_name";
        }

        // 使用原生 SQL 聚合（MyBatis-Plus 不支持 groupBy 动态字段）
        List<Map<String, Object>> rows = costRecordMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CostRecord>()
                        .eq("workspace_id", workspaceId)
                        .between("recorded_at", rangeStart, rangeEnd)
                        .select(groupField + " as label",
                                "SUM(cost) as total_cost",
                                "SUM(token_input) as total_input",
                                "SUM(token_output) as total_output")
                        .groupBy(groupField)
                        .orderByDesc("total_cost")
        );

        BigDecimal grandTotal = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            Object costObj = row.get("total_cost");
            if (costObj instanceof BigDecimal) {
                grandTotal = grandTotal.add((BigDecimal) costObj);
            }
        }

        List<CostBreakdownVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            CostBreakdownVO vo = new CostBreakdownVO();
            vo.setLabel(row.get("label") != null ? row.get("label").toString() : "未知");
            BigDecimal cost = row.get("total_cost") instanceof BigDecimal
                    ? (BigDecimal) row.get("total_cost") : BigDecimal.ZERO;
            vo.setCost(cost);
            vo.setPercent(grandTotal.compareTo(BigDecimal.ZERO) > 0
                    ? cost.multiply(new BigDecimal("100")).divide(grandTotal, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            vo.setTokenInput(row.get("total_input") instanceof Number
                    ? ((Number) row.get("total_input")).longValue() : 0L);
            vo.setTokenOutput(row.get("total_output") instanceof Number
                    ? ((Number) row.get("total_output")).longValue() : 0L);
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<CostTrendVO> getCostTrend(String period, String granularity) {
        Long workspaceId = SecurityUtils.currentWorkspaceId();
        LocalDate now = LocalDate.now();

        // 时间范围
        int days = "7d".equals(period) ? 7 : 30;
        LocalDateTime rangeStart = now.minusDays(days).atStartOfDay();
        LocalDateTime rangeEnd = now.atTime(LocalTime.MAX);

        List<Map<String, Object>> rows = costRecordMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CostRecord>()
                        .eq("workspace_id", workspaceId)
                        .between("recorded_at", rangeStart, rangeEnd)
                        .select("DATE(recorded_at) as date",
                                "SUM(cost) as total_cost",
                                "SUM(total_tokens) as total_tokens")
                        .groupBy("DATE(recorded_at)")
                        .orderByAsc("date")
        );

        // 填充缺失日期
        Map<String, CostTrendVO> dateMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String date = row.get("date").toString();
            CostTrendVO vo = new CostTrendVO();
            vo.setDate(date);
            vo.setCost(row.get("total_cost") instanceof BigDecimal
                    ? (BigDecimal) row.get("total_cost") : BigDecimal.ZERO);
            vo.setTokens(row.get("total_tokens") instanceof Number
                    ? ((Number) row.get("total_tokens")).longValue() : 0L);
            dateMap.put(date, vo);
        }

        List<CostTrendVO> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = days; i >= 0; i--) {
            String dateStr = now.minusDays(i).format(fmt);
            if (dateMap.containsKey(dateStr)) {
                result.add(dateMap.get(dateStr));
            } else {
                CostTrendVO vo = new CostTrendVO();
                vo.setDate(dateStr);
                vo.setCost(BigDecimal.ZERO);
                vo.setTokens(0L);
                result.add(vo);
            }
        }
        return result;
    }

    @Override
    public PageResult<CostRecordVO> getCostRecords(int page, int pageSize) {
        Long workspaceId = SecurityUtils.currentWorkspaceId();

        Page<CostRecord> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<CostRecord> qw = new LambdaQueryWrapper<CostRecord>();
        qw.eq(CostRecord::getWorkspaceId, workspaceId);
        qw.orderByDesc(CostRecord::getRecordedAt);

        Page<CostRecord> result = costRecordMapper.selectPage(pageParam, qw);

        List<CostRecordVO> voList = new ArrayList<>();
        for (CostRecord r : result.getRecords()) {
            CostRecordVO vo = new CostRecordVO();
            vo.setRecordId(r.getId());
            vo.setAgentId(r.getAgentId());
            vo.setAgentName(r.getAgentName());
            vo.setSessionId(r.getSessionId());
            vo.setModelProvider(r.getModelProvider());
            vo.setModelName(r.getModelName());
            vo.setTokenInput(r.getTokenInput());
            vo.setTokenOutput(r.getTokenOutput());
            vo.setCost(r.getCost());
            vo.setUserId(r.getUserId());
            vo.setUserName(r.getUserName());
            vo.setCreatedAt(r.getRecordedAt());
            voList.add(vo);
        }
        return PageResult.of(result, voList);
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
