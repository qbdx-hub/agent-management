package com.agentmanagement.task;

import com.agentmanagement.entity.AlertRecord;
import com.agentmanagement.entity.AlertRule;
import com.agentmanagement.entity.CostRecord;
import com.agentmanagement.entity.ErrorLog;
import com.agentmanagement.entity.Session;
import com.agentmanagement.mapper.AlertRecordMapper;
import com.agentmanagement.mapper.AlertRuleMapper;
import com.agentmanagement.mapper.CostRecordMapper;
import com.agentmanagement.mapper.ErrorLogMapper;
import com.agentmanagement.mapper.SessionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警评估定时任务 —— 每分钟评估一次启用的告警规则。
 * <p>指标口径（与前端 AlertConfig 的指标选项一致）：
 * <ul>
 *   <li>success_rate：评估窗口内 成功调用(cost_record) / 总调用(成功+失败 error_log)，0-1</li>
 *   <li>error_rate：1 - success_rate，0-1</li>
 *   <li>p99_latency：评估窗口内 session.latency 的 P99（ms）</li>
 *   <li>daily_tokens：今日 0 点起 cost_record 的 token 总量</li>
 * </ul>
 * <p>状态机：指标越限且该规则无未恢复告警时生成 alert_record(status=triggered)；
 * 指标恢复后自动将其置为 resolved，避免同一规则重复刷告警。
 * 评估窗口取规则的 duration（0m/未知按 1 小时）；日累计类指标固定从今日 0 点起算。
 */
@Slf4j
@Component
public class AlertEvaluationTask {

    @Autowired
    private AlertRuleMapper alertRuleMapper;

    @Autowired
    private AlertRecordMapper alertRecordMapper;

    @Autowired
    private CostRecordMapper costRecordMapper;

    @Autowired
    private ErrorLogMapper errorLogMapper;

    @Autowired
    private SessionMapper sessionMapper;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void evaluate() {
        List<AlertRule> rules = alertRuleMapper.selectList(new LambdaQueryWrapper<AlertRule>()
                .eq(AlertRule::getEnabled, 1));
        for (AlertRule rule : rules) {
            try {
                evaluateRule(rule);
            } catch (Exception e) {
                // 单条规则失败不影响其余规则评估
                log.warn("告警规则评估失败: ruleId={}, name={}", rule.getId(), rule.getName(), e);
            }
        }
    }

    private void evaluateRule(AlertRule rule) {
        Double value = computeMetric(rule);
        if (value == null) {
            // 窗口内无数据：不触发也不恢复，保持现状
            return;
        }
        double threshold = rule.getThreshold() != null ? rule.getThreshold().doubleValue() : 0.0;
        boolean breached = compare(value, rule.getCondition(), threshold);

        // 该规则当前未恢复的告警（同一时刻最多一条）
        AlertRecord open = alertRecordMapper.selectOne(new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getRuleId, rule.getId())
                .eq(AlertRecord::getStatus, "triggered")
                .orderByDesc(AlertRecord::getTriggeredAt)
                .last("LIMIT 1"));

        if (breached && open == null) {
            AlertRecord record = new AlertRecord();
            record.setRuleId(rule.getId());
            record.setRuleName(rule.getName());
            record.setWorkspaceId(rule.getWorkspaceId());
            record.setAgentId("agent".equals(rule.getTargetType()) ? rule.getTargetId() : null);
            record.setSeverity(rule.getSeverity() != null ? rule.getSeverity() : "warning");
            record.setMessage(rule.getName() + "：当前值 " + formatValue(value)
                    + " " + conditionText(rule.getCondition()) + " 阈值 " + formatValue(threshold));
            record.setCurrentValue(formatValue(value));
            record.setThresholdValue(rule.getThreshold() != null ? rule.getThreshold().toPlainString() : null);
            record.setStatus("triggered");
            record.setTriggeredAt(LocalDateTime.now());
            alertRecordMapper.insert(record);
            log.info("告警触发: rule={}, 当前值={}, 条件={}{}", rule.getName(), value, rule.getCondition(), threshold);
        } else if (!breached && open != null) {
            alertRecordMapper.update(null, new LambdaUpdateWrapper<AlertRecord>()
                    .eq(AlertRecord::getId, open.getId())
                    .set(AlertRecord::getStatus, "resolved")
                    .set(AlertRecord::getResolvedAt, LocalDateTime.now()));
            log.info("告警恢复: rule={}, 当前值={}", rule.getName(), value);
        }
    }

    /** 计算规则指标的当前值；窗口内无数据返回 null（不参与评估） */
    private Double computeMetric(AlertRule rule) {
        Long workspaceId = rule.getWorkspaceId();
        boolean agentScoped = "agent".equals(rule.getTargetType()) && rule.getTargetId() != null;
        String metric = rule.getMetric();

        // 日累计类指标：固定从今日 0 点起算，与 duration 无关
        if ("daily_tokens".equals(metric)) {
            List<CostRecord> records = costRecordMapper.selectList(new LambdaQueryWrapper<CostRecord>()
                    .eq(CostRecord::getWorkspaceId, workspaceId)
                    .eq(agentScoped, CostRecord::getAgentId, rule.getTargetId())
                    .ge(CostRecord::getRecordedAt, LocalDate.now().atStartOfDay()));
            long tokens = 0;
            for (CostRecord r : records) {
                tokens += r.getTotalTokens() != null ? r.getTotalTokens() : 0;
            }
            return (double) tokens;
        }

        // 比率 / 延迟类指标：按规则 duration 指定的窗口评估
        LocalDateTime windowStart = LocalDateTime.now().minus(windowOf(rule.getDuration()));

        if ("success_rate".equals(metric) || "error_rate".equals(metric)) {
            long success = costRecordMapper.selectCount(new LambdaQueryWrapper<CostRecord>()
                    .eq(CostRecord::getWorkspaceId, workspaceId)
                    .eq(agentScoped, CostRecord::getAgentId, rule.getTargetId())
                    .ge(CostRecord::getRecordedAt, windowStart));
            long errors = errorLogMapper.selectCount(new LambdaQueryWrapper<ErrorLog>()
                    .eq(ErrorLog::getWorkspaceId, workspaceId)
                    .eq(agentScoped, ErrorLog::getAgentId, rule.getTargetId())
                    .ge(ErrorLog::getOccurredAt, windowStart));
            if (success + errors == 0) {
                return null;
            }
            double rate = (double) success / (success + errors);
            return "success_rate".equals(metric) ? rate : 1.0 - rate;
        }

        if ("p99_latency".equals(metric)) {
            List<Session> sessions = sessionMapper.selectList(new LambdaQueryWrapper<Session>()
                    .eq(Session::getWorkspaceId, workspaceId)
                    .eq(agentScoped, Session::getAgentId, rule.getTargetId())
                    .ge(Session::getCreatedAt, windowStart)
                    .isNotNull(Session::getLatency)
                    .orderByDesc(Session::getLatency));
            if (sessions.isEmpty()) {
                return null;
            }
            int idx = Math.min((int) Math.ceil(sessions.size() * 0.99) - 1, sessions.size() - 1);
            return sessions.get(Math.max(idx, 0)).getLatency() != null
                    ? sessions.get(Math.max(idx, 0)).getLatency().doubleValue() : null;
        }

        // 未知指标不评估
        return null;
    }

    /** 规则 duration → 评估窗口长度；"0m"（立即）与未知值按 1 小时 */
    private Duration windowOf(String duration) {
        if ("5m".equals(duration)) {
            return Duration.ofMinutes(5);
        }
        if ("1d".equals(duration)) {
            return Duration.ofDays(1);
        }
        return Duration.ofHours(1);
    }

    private boolean compare(double value, String condition, double threshold) {
        if ("lt".equals(condition)) {
            return value < threshold;
        }
        if ("lte".equals(condition)) {
            return value <= threshold;
        }
        if ("gt".equals(condition)) {
            return value > threshold;
        }
        if ("gte".equals(condition)) {
            return value >= threshold;
        }
        return false;
    }

    private String conditionText(String condition) {
        if ("lt".equals(condition)) {
            return "低于";
        }
        if ("lte".equals(condition)) {
            return "不高于";
        }
        if ("gt".equals(condition)) {
            return "高于";
        }
        if ("gte".equals(condition)) {
            return "不低于";
        }
        return condition != null ? condition : "";
    }

    /** 阈值/当前值格式化：比率保留 2 位小数，其余整量化（去尾零） */
    private String formatValue(double value) {
        if (value >= 0.0 && value <= 1.0 && value != Math.floor(value)) {
            return String.format("%.2f", value);
        }
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%.2f", value);
    }
}
