package com.agentmanagement.controller;

import com.agentmanagement.common.Result;
import com.agentmanagement.form.AlertRuleForm;
import com.agentmanagement.service.MonitorService;
import com.agentmanagement.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 监控面板 RESTful 接口（前缀 /api/v1 由 context-path 统一加）。
 * 对齐前端 api/monitor.ts 的全部端点。
 */
@RestController
@RequestMapping("/monitor")
public class MonitorController {

    @Autowired
    private MonitorService monitorService;

    /** GET /monitor/overview —— 监控概览 */
    @GetMapping("/overview")
    public Result<MonitorOverviewVO> getOverview(@RequestParam("period") String period) {
        return Result.success(monitorService.getOverview(period));
    }

    /** GET /monitor/token-trend —— Token 用量趋势 */
    @GetMapping("/token-trend")
    public Result<TokenTrendSummaryVO> getTokenTrend(@RequestParam("period") String period,
                                                      @RequestParam("granularity") String granularity) {
        return Result.success(monitorService.getTokenTrend(period, granularity));
    }

    /** GET /monitor/agent-health —— Agent 健康排行 */
    @GetMapping("/agent-health")
    public Result<List<AgentHealthVO>> getAgentHealth() {
        return Result.success(monitorService.getAgentHealth());
    }

    /** GET /monitor/errors —— 错误日志列表 */
    @GetMapping("/errors")
    public Result<List<ErrorLogVO>> getErrorLogs(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                  @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        return Result.success(monitorService.getErrorLogs(page, pageSize));
    }

    /** GET /monitor/alerts/rules —— 告警规则列表 */
    @GetMapping("/alerts/rules")
    public Result<List<AlertRuleVO>> getAlertRules() {
        return Result.success(monitorService.getAlertRules());
    }

    /** POST /monitor/alerts/rules —— 创建告警规则 */
    @PostMapping("/alerts/rules")
    public Result<AlertRuleVO> createAlertRule(@RequestBody AlertRuleForm form) {
        return Result.success(monitorService.createAlertRule(form));
    }

    /** PUT /monitor/alerts/rules/{id}/status —— 切换规则启用/禁用 */
    @PutMapping("/alerts/rules/{id}/status")
    public Result<Void> toggleAlertRuleStatus(@PathVariable("id") Long id,
                                               @RequestParam("enabled") Boolean enabled) {
        monitorService.toggleAlertRuleStatus(id, enabled);
        return Result.success();
    }

    /** DELETE /monitor/alerts/rules/{id} —— 删除告警规则 */
    @DeleteMapping("/alerts/rules/{id}")
    public Result<Void> deleteAlertRule(@PathVariable("id") Long id) {
        monitorService.deleteAlertRule(id);
        return Result.success();
    }

    /** GET /monitor/alerts/records —— 告警记录列表 */
    @GetMapping("/alerts/records")
    public Result<List<AlertRecordVO>> getAlertRecords(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                        @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        return Result.success(monitorService.getAlertRecords(page, pageSize));
    }
}
