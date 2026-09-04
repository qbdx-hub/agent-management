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

    @Autowired
    private CostRecordMapper costRecordMapper;

    @Autowired
    private MessageMapper messageMapper;

    // ====== 概览 ======

    @Override
    public MonitorOverviewVO getOverview(String period) {
        Long workspaceId = com.agentmanagement.security.SecurityUtils.currentWorkspaceId();

        MonitorOverviewVO vo = new MonitorOverviewVO();

        // 活跃 Agent 数（status = published）
        LambdaQueryWrapper<Agent> agentWrapper = new LambdaQueryWrapper<Agent>()
                .eq(Agent::getWorkspaceId, workspaceId)
                .eq(Agent::getStatus, "published");
        vo.setActiveAgentCount(agentMapper.selectCount(agentWrapper).intValue());

        // 执行中会话：session 对话结束仍保持 active，改按"近 15 分钟有新消息且状态 active"判定
        List<Object> recentIds = messageMapper.selectObjs(new LambdaQueryWrapper<Message>()
                .select(Message::getSessionId)
                .ge(Message::getCreatedAt, LocalDateTime.now().minusMinutes(15)));
        Set<Long> activeSessionIds = new HashSet<>();
        for (Object o : recentIds) {
            if (o instanceof Number) {
                activeSessionIds.add(((Number) o).longValue());
            }
        }
        long runningCount = activeSessionIds.isEmpty() ? 0
                : sessionMapper.selectCount(new LambdaQueryWrapper<Session>()
                        .eq(Session::getWorkspaceId, workspaceId)
                        .eq(Session::getStatus, "active")
                        .in(Session::getId, activeSessionIds));
        vo.setRunningTaskCount((int) runningCount);

        // 今日调用数与成功率：成功调用记 cost_record、失败调用记 error_log，两者相加即真实调用量
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todaySuccess = costRecordMapper.selectCount(new LambdaQueryWrapper<CostRecord>()
                .eq(CostRecord::getWorkspaceId, workspaceId)
                .ge(CostRecord::getRecordedAt, todayStart));
        long todayErrors = errorLogMapper.selectCount(new LambdaQueryWrapper<ErrorLog>()
                .eq(ErrorLog::getWorkspaceId, workspaceId)
                .ge(ErrorLog::getOccurredAt, todayStart));
        long todayCalls = todaySuccess + todayErrors;
        vo.setTodayCallCount(todayCalls);
        vo.setSuccessRate(todayCalls > 0 ? todaySuccess * 100.0 / todayCalls : null);

        // 平均延迟 & P99（session.latency 为每次 AI 回复的真实耗时）
        List<Session> todaySessions = sessionMapper.selectList(new LambdaQueryWrapper<Session>()
                .eq(Session::getWorkspaceId, workspaceId)
                .ge(Session::getCreatedAt, todayStart)
                .isNotNull(Session::getLatency)
                .orderByDesc(Session::getLatency));

        if (!todaySessions.isEmpty()) {
            Double avg = avgLatency(workspaceId, null, todayStart, LocalDateTime.now());
            vo.setAvgLatencyMs(avg != null ? (int) Math.round(avg) : 0);
            // P99：取第 99 百分位
            int p99Index = (int) Math.ceil(todaySessions.size() * 0.99) - 1;
            p99Index = Math.min(p99Index, todaySessions.size() - 1);
            vo.setP99LatencyMs(todaySessions.get(p99Index).getLatency());
        } else {
            vo.setAvgLatencyMs(0);
            vo.setP99LatencyMs(0);
        }

        // 今日总 Token：从 cost_record 聚合真实用量
        long totalTokens = 0;
        for (CostRecord cr : costRecordMapper.selectList(new LambdaQueryWrapper<CostRecord>()
                .eq(CostRecord::getWorkspaceId, workspaceId)
                .ge(CostRecord::getRecordedAt, todayStart)
                .isNotNull(CostRecord::getTotalTokens))) {
            totalTokens += cr.getTotalTokens() != null ? cr.getTotalTokens() : 0;
        }
        vo.setTotalTokensToday(totalTokens);

        // 趋势：与昨天对比（调用量的相对变化、成功率的绝对差、延迟的相对变化）
        LocalDateTime yesterdayStart = LocalDate.now().minusDays(1).atStartOfDay();
        long yesterdaySuccess = costRecordMapper.selectCount(new LambdaQueryWrapper<CostRecord>()
                .eq(CostRecord::getWorkspaceId, workspaceId)
                .ge(CostRecord::getRecordedAt, yesterdayStart)
                .lt(CostRecord::getRecordedAt, todayStart));
        long yesterdayErrors = errorLogMapper.selectCount(new LambdaQueryWrapper<ErrorLog>()
                .eq(ErrorLog::getWorkspaceId, workspaceId)
                .ge(ErrorLog::getOccurredAt, yesterdayStart)
                .lt(ErrorLog::getOccurredAt, todayStart));
        long yesterdayCalls = yesterdaySuccess + yesterdayErrors;

        MonitorOverviewVO.Trends trends = new MonitorOverviewVO.Trends();
        trends.setCallCountChange(yesterdayCalls > 0 ? (double) (todayCalls - yesterdayCalls) / yesterdayCalls : 0.0);
        Double todayRate = vo.getSuccessRate();
        Double yesterdayRate = yesterdayCalls > 0 ? yesterdaySuccess * 100.0 / yesterdayCalls : null;
        trends.setSuccessRateChange(todayRate != null && yesterdayRate != null
                ? todayRate - yesterdayRate : null);

        Double todayAvgLatency = avgLatency(workspaceId, null, todayStart, LocalDateTime.now());
        Double yesterdayAvgLatency = avgLatency(workspaceId, null, yesterdayStart, todayStart);
        trends.setLatencyChange(todayAvgLatency != null && yesterdayAvgLatency != null && yesterdayAvgLatency > 0
                ? (todayAvgLatency - yesterdayAvgLatency) / yesterdayAvgLatency : 0.0);
        vo.setTrends(trends);

        return vo;
    }

    // ====== Token 趋势 ======

    @Override
    public TokenTrendSummaryVO getTokenTrend(String period, String granularity) {
        Long workspaceId = com.agentmanagement.security.SecurityUtils.currentWorkspaceId();

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

        // 查询时间范围内的真实调用记录（cost_record 含 input/output 分项与真实费用）
        List<CostRecord> records = costRecordMapper.selectList(new LambdaQueryWrapper<CostRecord>()
                .eq(CostRecord::getWorkspaceId, workspaceId)
                .ge(CostRecord::getRecordedAt, startTime));

        // 按 hour / day 聚合
        List<TokenTrendPointVO> series;
        if ("hour".equals(granularity)) {
            Map<Integer, Long> inputByHour = new LinkedHashMap<>();
            Map<Integer, Long> outputByHour = new LinkedHashMap<>();
            // 初始化 24 小时
            for (int h = 0; h < 24; h++) {
                inputByHour.put(h, 0L);
                outputByHour.put(h, 0L);
            }
            for (CostRecord r : records) {
                if (r.getRecordedAt() == null) {
                    continue;
                }
                int hour = r.getRecordedAt().getHour();
                inputByHour.merge(hour, r.getTokenInput() != null ? r.getTokenInput() : 0L, Long::sum);
                outputByHour.merge(hour, r.getTokenOutput() != null ? r.getTokenOutput() : 0L, Long::sum);
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
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
            Map<LocalDate, Long> inputByDay = new LinkedHashMap<>();
            Map<LocalDate, Long> outputByDay = new LinkedHashMap<>();
            for (CostRecord r : records) {
                if (r.getRecordedAt() == null) {
                    continue;
                }
                LocalDate day = r.getRecordedAt().toLocalDate();
                inputByDay.merge(day, r.getTokenInput() != null ? r.getTokenInput() : 0L, Long::sum);
                outputByDay.merge(day, r.getTokenOutput() != null ? r.getTokenOutput() : 0L, Long::sum);
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

        // summary（费用为 cost_record 中按各 Agent 配置单价算出的真实总额）
        long totalInput = series.stream().mapToLong(TokenTrendPointVO::getInput).sum();
        long totalOutput = series.stream().mapToLong(TokenTrendPointVO::getOutput).sum();
        double totalCost = 0;
        for (CostRecord r : records) {
            if (r.getCost() != null) {
                totalCost += r.getCost().doubleValue();
            }
        }

        TokenTrendSummaryVO result = new TokenTrendSummaryVO();
        result.setSeries(series);
        TokenTrendSummaryVO.Summary summary = new TokenTrendSummaryVO.Summary();
        summary.setTotalInput(totalInput);
        summary.setTotalOutput(totalOutput);
        summary.setTotalCost(totalCost);
        result.setSummary(summary);

        return result;
    }

    // ====== 图表聚合 ======

    @Override
    public MonitorChartsVO getCharts(String period) {
        Long workspaceId = com.agentmanagement.security.SecurityUtils.currentWorkspaceId();

        // 时间窗与桶粒度：today 按小时 24 桶，7d/30d 按天
        boolean hourly = "today".equals(period);
        int days = "30d".equals(period) ? 30 : 7;
        LocalDateTime start = hourly ? LocalDate.now().atStartOfDay()
                : LocalDate.now().minusDays(days - 1).atStartOfDay();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");

        // 预建桶（无数据的桶补 0，保证横轴连续）
        Map<String, MonitorChartsVO.CallTrendPoint> callBuckets = new LinkedHashMap<>();
        Map<String, MonitorChartsVO.CostTrendPoint> costBuckets = new LinkedHashMap<>();
        if (hourly) {
            for (int h = 0; h < 24; h++) {
                callBuckets.put(hourKey(h), newPoint(h + ":00"));
                MonitorChartsVO.CostTrendPoint p = new MonitorChartsVO.CostTrendPoint();
                p.setTime(h + ":00");
                p.setCost(0.0);
                costBuckets.put(hourKey(h), p);
            }
        } else {
            for (LocalDate d = start.toLocalDate(); !d.isAfter(LocalDate.now()); d = d.plusDays(1)) {
                String label = d.format(fmt);
                callBuckets.put(label, newPoint(label));
                MonitorChartsVO.CostTrendPoint p = new MonitorChartsVO.CostTrendPoint();
                p.setTime(label);
                p.setCost(0.0);
                costBuckets.put(label, p);
            }
        }

        List<CostRecord> costs = costRecordMapper.selectList(new LambdaQueryWrapper<CostRecord>()
                .eq(CostRecord::getWorkspaceId, workspaceId)
                .ge(CostRecord::getRecordedAt, start));
        for (CostRecord r : costs) {
            if (r.getRecordedAt() == null) {
                continue;
            }
            String key = hourly ? hourKey(r.getRecordedAt().getHour()) : r.getRecordedAt().toLocalDate().format(fmt);
            MonitorChartsVO.CallTrendPoint c = callBuckets.get(key);
            if (c != null) {
                c.setCalls(c.getCalls() + 1);
            }
            MonitorChartsVO.CostTrendPoint ct = costBuckets.get(key);
            if (ct != null && r.getCost() != null) {
                ct.setCost(ct.getCost() + r.getCost().doubleValue());
            }
        }

        List<ErrorLog> errs = errorLogMapper.selectList(new LambdaQueryWrapper<ErrorLog>()
                .eq(ErrorLog::getWorkspaceId, workspaceId)
                .ge(ErrorLog::getOccurredAt, start));
        for (ErrorLog e : errs) {
            if (e.getOccurredAt() == null) {
                continue;
            }
            String key = hourly ? hourKey(e.getOccurredAt().getHour()) : e.getOccurredAt().toLocalDate().format(fmt);
            MonitorChartsVO.CallTrendPoint c = callBuckets.get(key);
            if (c != null) {
                c.setErrors(c.getErrors() + 1);
            }
        }

        // Agent 调用分布 TOP5（cost_record 已冗余 agentName，无需回表）
        Map<Long, MonitorChartsVO.AgentCallDist> byAgent = new LinkedHashMap<>();
        for (CostRecord r : costs) {
            if (r.getAgentId() == null) {
                continue;
            }
            MonitorChartsVO.AgentCallDist d = byAgent.computeIfAbsent(r.getAgentId(), id -> {
                MonitorChartsVO.AgentCallDist n = new MonitorChartsVO.AgentCallDist();
                n.setAgentId(id);
                n.setAgentName(r.getAgentName() != null ? r.getAgentName() : "Agent #" + id);
                n.setCalls(0L);
                n.setTokens(0L);
                return n;
            });
            d.setCalls(d.getCalls() + 1);
            d.setTokens(d.getTokens() + (r.getTotalTokens() != null ? r.getTotalTokens() : 0L));
        }
        List<MonitorChartsVO.AgentCallDist> topAgents = byAgent.values().stream()
                .sorted(Comparator.comparingLong(MonitorChartsVO.AgentCallDist::getCalls).reversed())
                .limit(5)
                .collect(Collectors.toList());

        // 错误类型分布（按次数倒序，取 TOP6）
        Map<String, Long> byType = new LinkedHashMap<>();
        for (ErrorLog e : errs) {
            byType.merge(e.getErrorType() != null ? e.getErrorType() : "UNKNOWN", 1L, Long::sum);
        }
        List<MonitorChartsVO.ErrorTypeDist> typeDist = byType.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(6)
                .map(en -> {
                    MonitorChartsVO.ErrorTypeDist d = new MonitorChartsVO.ErrorTypeDist();
                    d.setErrorType(en.getKey());
                    d.setCount(en.getValue());
                    return d;
                })
                .collect(Collectors.toList());

        MonitorChartsVO vo = new MonitorChartsVO();
        vo.setCallTrend(new ArrayList<>(callBuckets.values()));
        vo.setCostTrend(new ArrayList<>(costBuckets.values()));
        vo.setAgentDistribution(topAgents);
        vo.setErrorTypeDistribution(typeDist);
        return vo;
    }

    private String hourKey(int h) {
        return String.format("%02d", h);
    }

    private MonitorChartsVO.CallTrendPoint newPoint(String time) {
        MonitorChartsVO.CallTrendPoint p = new MonitorChartsVO.CallTrendPoint();
        p.setTime(time);
        p.setCalls(0L);
        p.setErrors(0L);
        return p;
    }

    // ====== Agent 健康排行 ======

    @Override
    public List<AgentHealthVO> getAgentHealth() {
        Long workspaceId = com.agentmanagement.security.SecurityUtils.currentWorkspaceId();

        // 获取工作空间下所有 published 状态的 Agent
        List<Agent> agents = agentMapper.selectList(new LambdaQueryWrapper<Agent>()
                .eq(Agent::getWorkspaceId, workspaceId)
                .ne(Agent::getStatus, "archived"));

        LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
        List<AgentHealthVO> result = new ArrayList<>();

        for (Agent agent : agents) {
            AgentHealthVO vo = new AgentHealthVO();
            vo.setAgentId(agent.getId());
            vo.setAgentName(agent.getDisplayName() != null ? agent.getDisplayName() : agent.getName());

            // 最近 24h 调用次数：成功（cost_record）+ 失败（error_log）
            long successCount = costRecordMapper.selectCount(new LambdaQueryWrapper<CostRecord>()
                    .eq(CostRecord::getWorkspaceId, workspaceId)
                    .eq(CostRecord::getAgentId, agent.getId())
                    .ge(CostRecord::getRecordedAt, yesterday));
            long errorCount = errorLogMapper.selectCount(new LambdaQueryWrapper<ErrorLog>()
                    .eq(ErrorLog::getWorkspaceId, workspaceId)
                    .eq(ErrorLog::getAgentId, agent.getId())
                    .ge(ErrorLog::getOccurredAt, yesterday));
            long callCount = successCount + errorCount;
            vo.setCallCount24h((int) callCount);

            // 成功率：真实成功/失败比（24h 无调用视为满值，避免闲置 Agent 误报异常）；对外按百分数输出
            double rate = callCount > 0 ? (double) successCount / callCount : 1.0;
            vo.setSuccessRate(rate * 100.0);

            // 平均延迟：近 24h 该 Agent 各次 AI 回复的真实耗时均值
            Double avgLatency = avgLatency(workspaceId, agent.getId(), yesterday, LocalDateTime.now());
            vo.setAvgLatencyMs(avgLatency != null ? (int) Math.round(avgLatency) : 0);

            // 健康状态判断
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

        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 20;

        LambdaQueryWrapper<ErrorLog> wrapper = new LambdaQueryWrapper<ErrorLog>()
                .eq(ErrorLog::getWorkspaceId, workspaceId)
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

        List<AlertRule> rules = alertRuleMapper.selectList(new LambdaQueryWrapper<AlertRule>()
                .eq(AlertRule::getWorkspaceId, workspaceId)
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

        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 20;

        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getWorkspaceId, workspaceId)
                .orderByDesc(AlertRecord::getTriggeredAt)
                .last("LIMIT " + pageSize + " OFFSET " + (page - 1) * pageSize);

        List<AlertRecord> records = alertRecordMapper.selectList(wrapper);
        // LocalDateTime 无时区信息，格式化带 XXX（offset）会抛 UnsupportedTemporalTypeException
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

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

    /**
     * 指定时间窗内的平均消息延迟（session.latency 为每次 AI 回复耗时）。
     * agentId 传 null 表示全工作空间；窗口内无数据返回 null。
     */
    private Double avgLatency(Long workspaceId, Long agentId, LocalDateTime start, LocalDateTime end) {
        List<Session> sessions = sessionMapper.selectList(new LambdaQueryWrapper<Session>()
                .eq(Session::getWorkspaceId, workspaceId)
                .eq(agentId != null, Session::getAgentId, agentId)
                .ge(Session::getCreatedAt, start)
                .lt(Session::getCreatedAt, end)
                .isNotNull(Session::getLatency));
        if (sessions.isEmpty()) {
            return null;
        }
        long sum = 0;
        for (Session s : sessions) {
            sum += s.getLatency() != null ? s.getLatency() : 0;
        }
        return (double) sum / sessions.size();
    }

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
