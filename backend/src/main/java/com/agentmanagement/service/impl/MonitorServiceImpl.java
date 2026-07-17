package com.agentmanagement.service.impl;

import com.agentmanagement.entity.*;
import com.agentmanagement.form.AlertRuleForm;
import com.agentmanagement.mapper.*;
import com.agentmanagement.service.MonitorService;
import com.agentmanagement.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 监控服务实现 —— 聚合 agent、session、error_log、alert_rule、alert_record 等表数据。
 */
@Service
public class MonitorServiceImpl implements MonitorService {

    @Autowired
    private AgentMapper agentMapper;

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private ErrorLogMapper errorLogMapper;

    @Autowired
    private AlertRuleMapper alertRuleMapper;

    @Autowired
    private AlertRecordMapper alertRecordMapper;

    @Autowired
    private ActivityLogMapper activityLogMapper;

    // ====== 概览 ======

    @Override
    public MonitorOverviewVO getOverview(String period) {
        Long workspaceId = com.agentmanagement.security.SecurityUtils.currentWorkspaceId();
        Long userId = com.agentmanagement.security.SecurityUtils.currentUserId();

        MonitorOverviewVO vo = new MonitorOverviewVO();

        // 活跃 Agent 数（status = published）
        LambdaQueryWrapper<Agent> agentWrapper = new LambdaQueryWrapper<Agent>()
                .eq(Agent::getWorkspaceId, workspaceId)
                .eq(Agent::getCreatedBy, userId)
                .eq(Agent::getStatus, "published");
        vo.setActiveAgentCount(agentMapper.selectCount(agentWrapper).intValue());

        // 执行中任务数（session status = active）
        LambdaQueryWrapper<Session> activeWrapper = new LambdaQueryWrapper<Session>()
                .eq(Session::getWorkspaceId, workspaceId)
                .eq(Session::getCreatedBy, userId)
                .eq(Session::getStatus, "active");
        vo.setRunningTaskCount(sessionMapper.selectCount(activeWrapper).intValue());

        // 今日调用数
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LambdaQueryWrapper<Session> todayWrapper = new LambdaQueryWrapper<Session>()
                .eq(Session::getWorkspaceId, workspaceId)
                .eq(Session::getCreatedBy, userId)
                .ge(Session::getCreatedAt, todayStart);
        long todayCalls = sessionMapper.selectCount(todayWrapper);
        vo.setTodayCallCount(todayCalls);

        // 成功率：今日 session 中 completed / (completed + error)
        LambdaQueryWrapper<Session> completedWrapper = new LambdaQueryWrapper<Session>()
                .eq(Session::getWorkspaceId, workspaceId)
                .eq(Session::getCreatedBy, userId)
                .ge(Session::getCreatedAt, todayStart)
                .eq(Session::getStatus, "completed");
        long completedCount = sessionMapper.selectCount(completedWrapper);

        LambdaQueryWrapper<Session> errorWrapper = new LambdaQueryWrapper<Session>()
                .eq(Session::getWorkspaceId, workspaceId)
                .eq(Session::getCreatedBy, userId)
                .ge(Session::getCreatedAt, todayStart)
                .eq(Session::getStatus, "error");
        long errorCount = sessionMapper.selectCount(errorWrapper);

        long finishedTotal = completedCount + errorCount;
        vo.setSuccessRate(finishedTotal > 0 ? (double) completedCount / finishedTotal : 1.0);

        // 平均延迟 & P99
        List<Session> todaySessions = sessionMapper.selectList(new LambdaQueryWrapper<Session>()
                .eq(Session::getWorkspaceId, workspaceId)
                .eq(Session::getCreatedBy, userId)
                .ge(Session::getCreatedAt, todayStart)
                .isNotNull(Session::getLatency)
                .orderByDesc(Session::getLatency));

        if (!todaySessions.isEmpty()) {
            long sumLatency = 0;
            for (Session s : todaySessions) {
                sumLatency += s.getLatency() != null ? s.getLatency() : 0;
            }
            vo.setAvgLatencyMs((int) (sumLatency / todaySessions.size()));
            // P99：取第 99 百分位
            int p99Index = (int) Math.ceil(todaySessions.size() * 0.99) - 1;
            p99Index = Math.min(p99Index, todaySessions.size() - 1);
            vo.setP99LatencyMs(todaySessions.get(p99Index).getLatency());
        } else {
            vo.setAvgLatencyMs(0);
            vo.setP99LatencyMs(0);
        }

        // 今日总 Token
        long totalTokens = 0;
        for (Session s : sessionMapper.selectList(new LambdaQueryWrapper<Session>()
                .eq(Session::getWorkspaceId, workspaceId)
                .eq(Session::getCreatedBy, userId)
                .ge(Session::getCreatedAt, todayStart)
                .isNotNull(Session::getTotalTokens))) {
            totalTokens += s.getTotalTokens() != null ? s.getTotalTokens() : 0;
        }
        vo.setTotalTokensToday(totalTokens);

        // 趋势：与昨天对比
        LocalDateTime yesterdayStart = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime yesterdayEnd = todayStart;
        LambdaQueryWrapper<Session> yesterdayWrapper = new LambdaQueryWrapper<Session>()
                .eq(Session::getWorkspaceId, workspaceId)
                .eq(Session::getCreatedBy, userId)
                .ge(Session::getCreatedAt, yesterdayStart)
                .lt(Session::getCreatedAt, yesterdayEnd);
        long yesterdayCalls = sessionMapper.selectCount(yesterdayWrapper);

        MonitorOverviewVO.Trends trends = new MonitorOverviewVO.Trends();
        trends.setCallCountChange(yesterdayCalls > 0 ? (double) (todayCalls - yesterdayCalls) / yesterdayCalls : 0.0);
        trends.setSuccessRateChange(0.0);  // 简化实现
        trends.setLatencyChange(0.0);      // 简化实现
        vo.setTrends(trends);

        return vo;
    }

    // ====== Token 趋势 ======

    @Override
    public TokenTrendSummaryVO getTokenTrend(String period, String granularity) {
        Long workspaceId = com.agentmanagement.security.SecurityUtils.currentWorkspaceId();
        Long userId = com.agentmanagement.security.SecurityUtils.currentUserId();

        // 根据 period 确定时间范围
        LocalDateTime startTime;
        int points;
        if ("today".equals(period)) {
            startTime = LocalDate.now().atStartOfDay();
            points = 24;
        } else if ("7d".equals(period)) {
            startTime = LocalDate.now().minusDays(7).atStartOfDay();
            points = 7;
        } else {
            startTime = LocalDate.now().minusDays(30).atStartOfDay();
            points = 30;
        }

        // 查询时间范围内的 sessions
        List<Session> sessions = sessionMapper.selectList(new LambdaQueryWrapper<Session>()
                .eq(Session::getWorkspaceId, workspaceId)
                .eq(Session::getCreatedBy, userId)
                .ge(Session::getCreatedAt, startTime)
                .isNotNull(Session::getTotalTokens));

        // 按 hour / day 聚合
        List<TokenTrendPointVO> series;
        DateTimeFormatter formatter;
        if ("hour".equals(granularity)) {
            formatter = DateTimeFormatter.ofPattern("HH:mm");
            Map<Integer, Long> inputByHour = new LinkedHashMap<>();
            Map<Integer, Long> outputByHour = new LinkedHashMap<>();
            // 初始化 24 小时
            for (int h = 0; h < 24; h++) {
                inputByHour.put(h, 0L);
                outputByHour.put(h, 0L);
            }
            for (Session s : sessions) {
                int hour = s.getCreatedAt().getHour();
                // 简化：input 占 70%，output 占 30%（实际应从 step 级别区分）
                long total = s.getTotalTokens() != null ? s.getTotalTokens() : 0;
                inputByHour.merge(hour, (long) (total * 0.7), Long::sum);
                outputByHour.merge(hour, (long) (total * 0.3), Long::sum);
            }
            series = new ArrayList<>();
            for (int h = 0; h < 24; h++) {
                TokenTrendPointVO pt = new TokenTrendPointVO();
                pt.setTime(h + ":00");
                pt.setInput(inputByHour.getOrDefault(h, 0L));
                pt.setOutput(outputByHour.getOrDefault(h, 0L));
                series.add(pt);
            }
        } else {
            formatter = DateTimeFormatter.ofPattern("MM-dd");
            Map<LocalDate, Long> inputByDay = new LinkedHashMap<>();
            Map<LocalDate, Long> outputByDay = new LinkedHashMap<>();
            for (Session s : sessions) {
                LocalDate day = s.getCreatedAt().toLocalDate();
                long total = s.getTotalTokens() != null ? s.getTotalTokens() : 0;
                inputByDay.merge(day, (long) (total * 0.7), Long::sum);
                outputByDay.merge(day, (long) (total * 0.3), Long::sum);
            }
            series = new ArrayList<>();
            for (LocalDate d = startTime.toLocalDate(); !d.isAfter(LocalDate.now()); d = d.plusDays(1)) {
                TokenTrendPointVO pt = new TokenTrendPointVO();
                pt.setTime(d.format(formatter));
                pt.setInput(inputByDay.getOrDefault(d, 0L));
                pt.setOutput(outputByDay.getOrDefault(d, 0L));
                series.add(pt);
            }
        }

        // summary
        long totalInput = series.stream().mapToLong(TokenTrendPointVO::getInput).sum();
        long totalOutput = series.stream().mapToLong(TokenTrendPointVO::getOutput).sum();

        // 计算费用（简化：按 $0.002/1K tokens 粗算）
        double totalCost = (totalInput + totalOutput) / 1000.0 * 0.002;

        TokenTrendSummaryVO result = new TokenTrendSummaryVO();
        result.setSeries(series);
        TokenTrendSummaryVO.Summary summary = new TokenTrendSummaryVO.Summary();
        summary.setTotalInput(totalInput);
        summary.setTotalOutput(totalOutput);
        summary.setTotalCost(totalCost);
        result.setSummary(summary);

        return result;
    }

    // ====== Agent 健康排行 ======

    @Override
    public List<AgentHealthVO> getAgentHealth() {
        Long workspaceId = com.agentmanagement.security.SecurityUtils.currentWorkspaceId();
        Long userId = com.agentmanagement.security.SecurityUtils.currentUserId();

        // 获取当前用户的所有非归档 Agent
        List<Agent> agents = agentMapper.selectList(new LambdaQueryWrapper<Agent>()
                .eq(Agent::getWorkspaceId, workspaceId)
                .eq(Agent::getCreatedBy, userId)
                .ne(Agent::getStatus, "archived"));

        LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
        List<AgentHealthVO> result = new ArrayList<>();

        for (Agent agent : agents) {
            AgentHealthVO vo = new AgentHealthVO();
            vo.setAgentId(agent.getId());
            vo.setAgentName(agent.getDisplayName() != null ? agent.getDisplayName() : agent.getName());

            // 最近 24h 调用次数
            LambdaQueryWrapper<Session> sessionWrapper = new LambdaQueryWrapper<Session>()
                    .eq(Session::getWorkspaceId, workspaceId)
                    .eq(Session::getCreatedBy, userId)
                    .eq(Session::getAgentId, agent.getId())
                    .ge(Session::getCreatedAt, yesterday);
            long callCount = sessionMapper.selectCount(sessionWrapper);
            vo.setCallCount24h((int) callCount);

            // 成功率（从 Agent 冗余字段读取，或实时计算）
            if (agent.getSuccessRate() != null) {
                vo.setSuccessRate(agent.getSuccessRate().doubleValue() / 100.0);
            } else {
                vo.setSuccessRate(callCount > 0 ? 1.0 : 0.0);
            }

            // 平均延迟
            vo.setAvgLatencyMs(agent.getAvgLatencyMs() != null ? agent.getAvgLatencyMs() : 0);

            // 健康状态判断
            double rate = vo.getSuccessRate();
            if (rate >= 0.95) {
                vo.setStatus("healthy");
            } else if (rate >= 0.85) {
                vo.setStatus("warning");
            } else {
                vo.setStatus("critical");
            }

            // 最近错误摘要
            LambdaQueryWrapper<ErrorLog> errorWrapper = new LambdaQueryWrapper<ErrorLog>()
                    .eq(ErrorLog::getWorkspaceId, workspaceId)
                    .eq(ErrorLog::getAgentId, agent.getId())
                    .ge(ErrorLog::getOccurredAt, yesterday)
                    .orderByDesc(ErrorLog::getOccurredAt)
                    .last("LIMIT 1");
            ErrorLog lastError = errorLogMapper.selectOne(errorWrapper);
            if (lastError != null) {
                vo.setErrorSummary(lastError.getErrorMessage());
            } else {
                vo.setErrorSummary("无");
            }

            result.add(vo);
        }

        // 按成功率降序排列
        result.sort((a, b) -> Double.compare(b.getSuccessRate(), a.getSuccessRate()));
        return result;
    }

    // ====== 错误日志 ======

    @Override
    public List<ErrorLogVO> getErrorLogs(Integer page, Integer pageSize) {
        Long workspaceId = com.agentmanagement.security.SecurityUtils.currentWorkspaceId();
        Long userId = com.agentmanagement.security.SecurityUtils.currentUserId();

        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 20;

        // 获取当前用户的 Agent ID 列表
        List<Long> agentIds = agentMapper.selectList(new LambdaQueryWrapper<Agent>()
                .eq(Agent::getWorkspaceId, workspaceId)
                .eq(Agent::getCreatedBy, userId))
                .stream().map(Agent::getId).collect(Collectors.toList());

        LambdaQueryWrapper<ErrorLog> wrapper = new LambdaQueryWrapper<ErrorLog>()
                .eq(ErrorLog::getWorkspaceId, workspaceId)
                .in(agentIds.isEmpty() ? false : true, ErrorLog::getAgentId, agentIds)
                .orderByDesc(ErrorLog::getOccurredAt)
                .last("LIMIT " + pageSize + " OFFSET " + (page - 1) * pageSize);

        List<ErrorLog> logs = errorLogMapper.selectList(wrapper);
        List<ErrorLogVO> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        for (ErrorLog log : logs) {
            ErrorLogVO vo = new ErrorLogVO();
            vo.setErrorId(log.getId());
            vo.setAgentId(log.getAgentId());
            vo.setAgentName(log.getAgentName());
            vo.setSessionId(log.getSessionId());
            vo.setStepSequence(log.getStepId() != null ? log.getStepId().intValue() : 0);
            vo.setErrorType(log.getErrorType());
            vo.setErrorMessage(log.getErrorMessage());
            vo.setOccurredAt(log.getOccurredAt() != null ? log.getOccurredAt().format(fmt) : null);
            result.add(vo);
        }
        return result;
    }

    // ====== 告警规则 ======

    @Override
    public List<AlertRuleVO> getAlertRules() {
        Long workspaceId = com.agentmanagement.security.SecurityUtils.currentWorkspaceId();
        Long userId = com.agentmanagement.security.SecurityUtils.currentUserId();

        List<AlertRule> rules = alertRuleMapper.selectList(new LambdaQueryWrapper<AlertRule>()
                .eq(AlertRule::getWorkspaceId, workspaceId)
                .eq(AlertRule::getCreatedBy, userId)
                .orderByAsc(AlertRule::getId));

        List<AlertRuleVO> result = new ArrayList<>();
        for (AlertRule rule : rules) {
            result.add(toAlertRuleVO(rule));
        }
        return result;
    }

    @Override
    public AlertRuleVO createAlertRule(AlertRuleForm form) {
        Long workspaceId = com.agentmanagement.security.SecurityUtils.currentWorkspaceId();
        Long userId = com.agentmanagement.security.SecurityUtils.currentUserId();

        AlertRule rule = new AlertRule();
        rule.setWorkspaceId(workspaceId);
        rule.setName(form.getName());
        rule.setMetric(form.getMetric());
        rule.setTargetType(form.getTargetType());
        rule.setTargetId(form.getTargetId());
        rule.setCondition(form.getCondition());
        rule.setThreshold(BigDecimal.valueOf(form.getThreshold()));
        rule.setDuration(form.getDuration());
        rule.setSeverity(form.getSeverity());
        rule.setEnabled(form.getEnabled() != null ? (form.getEnabled() ? 1 : 0) : 1);
        rule.setNotifyChannels(form.getNotifyChannels());
        rule.setCreatedBy(userId);
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());

        alertRuleMapper.insert(rule);
        return toAlertRuleVO(rule);
    }

    @Override
    public void toggleAlertRuleStatus(Long ruleId, Boolean enabled) {
        alertRuleMapper.update(null, new LambdaUpdateWrapper<AlertRule>()
                .eq(AlertRule::getId, ruleId)
                .set(AlertRule::getEnabled, enabled ? 1 : 0)
                .set(AlertRule::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    public void deleteAlertRule(Long ruleId) {
        alertRuleMapper.deleteById(ruleId);
    }

    // ====== 告警记录 ======

    @Override
    public List<AlertRecordVO> getAlertRecords(Integer page, Integer pageSize) {
        Long workspaceId = com.agentmanagement.security.SecurityUtils.currentWorkspaceId();
        Long userId = com.agentmanagement.security.SecurityUtils.currentUserId();

        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 20;

        // 获取当前用户的告警规则 ID 列表
        List<Long> ruleIds = alertRuleMapper.selectList(new LambdaQueryWrapper<AlertRule>()
                .eq(AlertRule::getWorkspaceId, workspaceId)
                .eq(AlertRule::getCreatedBy, userId))
                .stream().map(AlertRule::getId).collect(Collectors.toList());

        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getWorkspaceId, workspaceId)
                .in(ruleIds.isEmpty() ? false : true, AlertRecord::getRuleId, ruleIds)
                .orderByDesc(AlertRecord::getTriggeredAt)
                .last("LIMIT " + pageSize + " OFFSET " + (page - 1) * pageSize);

        List<AlertRecord> records = alertRecordMapper.selectList(wrapper);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

        List<AlertRecordVO> result = new ArrayList<>();
        for (AlertRecord rec : records) {
            AlertRecordVO vo = new AlertRecordVO();
            vo.setRecordId(rec.getId());
            vo.setRuleName(rec.getRuleName());
            vo.setSeverity(rec.getSeverity());
            vo.setMessage(rec.getMessage());
            vo.setTriggeredAt(rec.getTriggeredAt() != null ? rec.getTriggeredAt().format(fmt) : null);
            vo.setResolvedAt(rec.getResolvedAt() != null ? rec.getResolvedAt().format(fmt) : null);
            vo.setStatus(rec.getStatus());
            result.add(vo);
        }
        return result;
    }

    // ====== 辅助方法 ======

    private AlertRuleVO toAlertRuleVO(AlertRule rule) {
        AlertRuleVO vo = new AlertRuleVO();
        vo.setId(rule.getId());
        vo.setName(rule.getName());
        vo.setMetric(rule.getMetric());
        vo.setTargetType(rule.getTargetType());
        vo.setTargetId(rule.getTargetId());
        vo.setCondition(rule.getCondition());
        vo.setThreshold(rule.getThreshold() != null ? rule.getThreshold().doubleValue() : null);
        vo.setDuration(rule.getDuration());
        vo.setSeverity(rule.getSeverity());
        vo.setEnabled(rule.getEnabled() != null && rule.getEnabled() == 1);
        vo.setNotifyChannels(rule.getNotifyChannels());
        return vo;
    }
}
