package com.agentmanagement.service;

import com.agentmanagement.vo.*;

import java.util.List;

/**
 * 监控服务接口 —— 提供概览、趋势、健康、告警、错误日志等功能。
 */
public interface MonitorService {

    /** 监控概览（活跃 Agent 数、执行中任务数、今日调用、成功率、延迟等） */
    MonitorOverviewVO getOverview(String period);

    /** Token 用量趋势（24h 或 7d / 30d） */
    TokenTrendSummaryVO getTokenTrend(String period, String granularity);

    /** Agent 健康排行 */
    List<AgentHealthVO> getAgentHealth();

    /** 错误日志列表 */
    List<ErrorLogVO> getErrorLogs(Integer page, Integer pageSize);

    /** 告警规则列表 */
    List<AlertRuleVO> getAlertRules();

    /** 创建告警规则 */
    AlertRuleVO createAlertRule(com.agentmanagement.form.AlertRuleForm form);

    /** 切换告警规则启用/禁用 */
    void toggleAlertRuleStatus(Long ruleId, Boolean enabled);

    /** 删除告警规则 */
    void deleteAlertRule(Long ruleId);

    /** 告警记录列表 */
    List<AlertRecordVO> getAlertRecords(Integer page, Integer pageSize);
}
