package com.agentmanagement.service.impl;

import com.agentmanagement.common.BusinessException;
import com.agentmanagement.common.PageResult;
import com.agentmanagement.common.ResultCode;
import com.agentmanagement.entity.Agent;
import com.agentmanagement.entity.Tool;
import com.agentmanagement.entity.ToolCallRecord;
import com.agentmanagement.form.ToolQueryForm;
import com.agentmanagement.form.ToolRegisterForm;
import com.agentmanagement.form.ToolUpdateForm;
import com.agentmanagement.mapper.AgentMapper;
import com.agentmanagement.mapper.ToolCallRecordMapper;
import com.agentmanagement.mapper.ToolMapper;
import com.agentmanagement.security.SecurityUtils;
import com.agentmanagement.service.ToolService;
import com.agentmanagement.service.builtin.BuiltinToolResult;
import com.agentmanagement.service.builtin.BuiltinToolService;
import com.agentmanagement.vo.ToolStatsVO;
import com.agentmanagement.vo.ToolSummaryVO;
import com.agentmanagement.vo.ToolTestResultVO;
import com.agentmanagement.vo.ToolVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ToolServiceImpl extends ServiceImpl<ToolMapper, Tool> implements ToolService {

    @Autowired
    private ToolMapper toolMapper;

    @Autowired
    private ToolCallRecordMapper toolCallRecordMapper;

    @Autowired
    private AgentMapper agentMapper;

    /** 内置工具执行器（@Lazy 断环：BuiltinToolServiceImpl 反向依赖本类回填统计） */
    @Autowired
    @Lazy
    private BuiltinToolService builtinToolService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 测试专用 HTTP 客户端（每次请求按工具配置的超时新建 call 配置，客户端复用连接池） */
    private final OkHttpClient testHttpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();

    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    /** 响应体入库/返回的最大长度，防止超大响应拖垮 DB 与前端 */
    private static final int MAX_BODY_LENGTH = 64 * 1024;

    /** 工具分类 → 中文标签（对齐前端 utils/constants.ts 的 TOOL_CATEGORY_MAP） */
    private static final Map<String, String> CATEGORY_LABELS = new HashMap<String, String>();
    static {
        CATEGORY_LABELS.put("search", "搜索");
        CATEGORY_LABELS.put("compute", "计算");
        CATEGORY_LABELS.put("operate", "操作");
        CATEGORY_LABELS.put("perceive", "感知");
        CATEGORY_LABELS.put("notify", "通知");
        CATEGORY_LABELS.put("custom", "自定义");
    }

    @Override
    public PageResult<ToolSummaryVO> pageTools(ToolQueryForm form) {
        // 工具市场全局共享：不做账户/工作空间隔离，所有账户可见并可绑定（产品决策 2026-09-03）
        Page<Tool> page = new Page<Tool>(form.getPage(), form.getPageSize());
        LambdaQueryWrapper<Tool> qw = new LambdaQueryWrapper<Tool>();
        if (StringUtils.hasText(form.getKeyword())) {
            // keyword 同时匹配 name 与 displayName
            qw.and(w -> w.like(Tool::getName, form.getKeyword())
                    .or().like(Tool::getDisplayName, form.getKeyword()));
        }
        qw.eq(StringUtils.hasText(form.getCategory()), Tool::getCategory, form.getCategory());
        qw.orderByDesc(Tool::getCreatedAt);

        Page<Tool> result = toolMapper.selectPage(page, qw);
        List<ToolSummaryVO> list = new ArrayList<ToolSummaryVO>();
        for (Tool tool : result.getRecords()) {
            list.add(toSummaryVO(tool));
        }
        return PageResult.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public ToolVO getToolDetail(Long id) {
        return toDetailVO(requireToolInWorkspace(id));
    }

    @Override
    public ToolVO registerTool(ToolRegisterForm form) {
        Long workspaceId = SecurityUtils.currentWorkspaceId();
        Long currentUserId = SecurityUtils.currentUserId();

        Tool tool = new Tool();
        tool.setWorkspaceId(workspaceId);
        tool.setCreatedBy(currentUserId);
        tool.setName(form.getName());
        tool.setDisplayName(form.getDisplayName());
        tool.setDescription(form.getDescription());
        tool.setCategory(StringUtils.hasText(form.getCategory()) ? form.getCategory() : "custom");
        tool.setIcon(form.getIcon());
        tool.setType(StringUtils.hasText(form.getType()) ? form.getType() : "api");
        // endpoint 嵌套 → 扁平落库
        if (form.getEndpoint() != null) {
            ToolRegisterForm.Endpoint ep = form.getEndpoint();
            tool.setEndpointUrl(ep.getUrl());
            tool.setMethod(ep.getMethod());
            tool.setHeaders(ep.getHeaders());
            tool.setEndpointTimeout(ep.getTimeoutMs());
        }
        tool.setParameters(form.getParameters());
        tool.setResponseMapping(form.getResponseMapping());
        tool.setCredentialRef(form.getCredentialRef());
        tool.setRetryOnFail(toInt(form.getRetryOnFail()));
        tool.setMaxRetries(form.getMaxRetries());
        // version/status/authType/统计等走 DB 默认值
        toolMapper.insert(tool);

        // 重新查询拿 DB 填充的默认值，再组装 VO
        return toDetailVO(toolMapper.selectById(tool.getId()));
    }

    @Override
    public void updateTool(Long id, ToolUpdateForm form) {
        Tool tool = requireToolInWorkspace(id);
        Tool update = new Tool();
        update.setId(tool.getId());
        update.setName(form.getName());
        update.setDisplayName(form.getDisplayName());
        update.setDescription(form.getDescription());
        update.setCategory(form.getCategory());
        update.setIcon(form.getIcon());
        update.setType(form.getType());
        if (form.getEndpoint() != null) {
            ToolRegisterForm.Endpoint ep = form.getEndpoint();
            update.setEndpointUrl(ep.getUrl());
            update.setMethod(ep.getMethod());
            update.setHeaders(ep.getHeaders());
            update.setEndpointTimeout(ep.getTimeoutMs());
        }
        update.setParameters(form.getParameters());
        update.setResponseMapping(form.getResponseMapping());
        update.setCredentialRef(form.getCredentialRef());
        if (form.getRetryOnFail() != null) {
            update.setRetryOnFail(toInt(form.getRetryOnFail()));
        }
        update.setMaxRetries(form.getMaxRetries());
        // updateById 默认仅更新非 null 字段，实现部分更新语义
        toolMapper.updateById(update);
    }

    @Override
    public void removeTool(Long id) {
        Tool tool = requireToolInWorkspace(id);
        toolMapper.deleteById(tool.getId());
    }

    // ==================== 连通性测试与统计 ====================

    @Override
    public ToolTestResultVO testTool(Long id, Map<String, Object> parameters) {
        return testTool(id, parameters, SecurityUtils.currentWorkspaceId(), null, null);
    }

    @Override
    public ToolTestResultVO testTool(Long id, Map<String, Object> parameters,
                                     Long workspaceId, Long agentId, Long sessionId) {
        // 工具全局共享：仅校验存在性（workspaceId 参数保留以兼容调用方，供无 SecurityContext 的异步线程使用）
        Tool tool = toolMapper.selectById(id);
        if (tool == null) {
            throw new BusinessException(ResultCode.TOOL_NOT_FOUND);
        }

        if (!"api".equalsIgnoreCase(tool.getType())) {
            if ("builtin".equalsIgnoreCase(tool.getType())) {
                // 内置工具真实试运行：与对话调用同一 execute()（空间禁用策略同样生效），
                // 固定沙箱内执行（session=null → ws-{id}/session-null 工作台目录），调用记录与统计同 API 工具
                long startMs = System.currentTimeMillis();
                BuiltinToolResult r = builtinToolService.execute(tool, parameters, agentId, sessionId, workspaceId, false);
                int latencyMs = (int) (System.currentTimeMillis() - startMs);
                ToolTestResultVO info = new ToolTestResultVO();
                info.setSuccess(r.isSuccess());
                info.setLatencyMs(latencyMs);
                info.setResponseStatus(r.isSuccess() ? 200 : 500);
                info.setResponseBody(r.isSuccess() ? r.getOutput() : r.getErrorMessage());
                info.setMappedOutput(r.getOutput());
                return info;
            }
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "类型为 " + tool.getType() + " 的工具暂不支持连通性测试，仅支持 API 类型工具");
        }
        if (!StringUtils.hasText(tool.getEndpointUrl())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "工具未配置端点 URL，请先在编辑页补全");
        }

        String method = StringUtils.hasText(tool.getMethod()) ? tool.getMethod().toUpperCase() : "GET";
        boolean hasBody = !"GET".equals(method) && !"HEAD".equals(method);

        // 组装请求：GET 参数拼 query string，其余拼 JSON body
        String requestBody = null;
        String url = tool.getEndpointUrl();
        if (parameters != null && !parameters.isEmpty()) {
            if (hasBody) {
                requestBody = writeJson(parameters);
            } else {
                StringBuilder qs = new StringBuilder();
                for (Map.Entry<String, Object> e : parameters.entrySet()) {
                    if (qs.length() > 0) {
                        qs.append("&");
                    }
                    // 查询串必须 URL 编码：中文/空格/特殊字符不做编码会坏请求（模型生成参数必踩）
                    qs.append(urlEncode(e.getKey())).append("=")
                            .append(e.getValue() != null ? urlEncode(String.valueOf(e.getValue())) : "");
                }
                url = url + (url.contains("?") ? "&" : "?") + qs;
            }
        }

        Request.Builder rb = new Request.Builder().url(url);
        // 配置的请求头
        if (tool.getHeaders() != null) {
            for (Map.Entry<String, String> h : tool.getHeaders().entrySet()) {
                rb.header(h.getKey(), h.getValue());
            }
        }
        applyAuth(rb, tool);

        if (hasBody) {
            rb.method(method, RequestBody.create(requestBody != null ? requestBody : "{}", JSON_TYPE));
        }

        ToolTestResultVO result = new ToolTestResultVO();
        result.setRequestUrl(url);
        result.setRequestBody(requestBody);
        result.setSuccess(false);
        result.setResponseStatus(0);
        result.setResponseBody("");
        result.setMappedOutput("");

        long startMs = System.currentTimeMillis();
        String errorMessage = null;
        String responseBody = "";
        Integer statusCode = null;
        try {
            int timeoutMs = tool.getEndpointTimeout() != null ? tool.getEndpointTimeout() : 10_000;
            // 按工具配置的超时派生客户端（与 testHttpClient 共享连接池）
            OkHttpClient client = testHttpClient.newBuilder()
                    .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .build();
            try (Response response = client.newCall(rb.build()).execute()) {
                statusCode = response.code();
                responseBody = response.body() != null ? response.body().string() : "";
            }
        } catch (Exception e) {
            errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.warn("工具测试请求失败: toolId={}, url={}", tool.getId(), url, e);
        }
        int latencyMs = (int) (System.currentTimeMillis() - startMs);

        boolean success = statusCode != null && statusCode >= 200 && statusCode < 300;
        result.setSuccess(success);
        result.setLatencyMs(latencyMs);
        if (statusCode != null) {
            result.setResponseStatus(statusCode);
        }
        result.setResponseBody(truncate(responseBody, MAX_BODY_LENGTH));
        if (success) {
            result.setMappedOutput(truncate(applyMapping(responseBody, tool.getResponseMapping()), MAX_BODY_LENGTH));
        }

        // 落调用记录（失败也记录，监控与统计依赖真实失败样本）
        try {
            ToolCallRecord record = new ToolCallRecord();
            record.setToolId(tool.getId());
            record.setAgentId(agentId);
            record.setSessionId(sessionId);
            record.setParams(parameters);
            record.setSuccess(success ? 1 : 0);
            record.setLatencyMs(latencyMs);
            record.setResultSummary(statusCode != null
                    ? "HTTP " + statusCode + "，" + responseBody.length() + " 字节" : "请求异常");
            if (errorMessage != null) {
                record.setErrorMessage(truncate(errorMessage, 1000));
            }
            record.setCreatedAt(LocalDateTime.now());
            toolCallRecordMapper.insert(record);
        } catch (Exception re) {
            log.warn("tool_call_record 写入失败: toolId={}", tool.getId(), re);
        }

        // 回填工具统计与状态（由调用记录实时聚合）
        refreshToolStats(tool.getId(), success);

        return result;
    }

    @Override
    public void refreshStatsAfterCall(Long toolId, boolean success) {
        refreshToolStats(toolId, success);
    }

    @Override
    public ToolStatsVO getToolStats(Long id) {
        requireToolInWorkspace(id);
        List<ToolCallRecord> records = toolCallRecordMapper.selectList(new LambdaQueryWrapper<ToolCallRecord>()
                .eq(ToolCallRecord::getToolId, id)
                .orderByAsc(ToolCallRecord::getCreatedAt));

        ToolStatsVO vo = new ToolStatsVO();
        vo.setTotalCalls((long) records.size());
        long successCount = records.stream()
                .filter(r -> r.getSuccess() != null && r.getSuccess() == 1).count();
        // 成功率：百分数口径（0-100），无调用数据返回 null
        vo.setSuccessRate(records.isEmpty() ? null : successCount * 100.0 / records.size());
        List<Integer> latencies = latenciesOf(records);
        vo.setAvgLatencyMs(latencies.isEmpty() ? 0
                : (int) Math.round(latencies.stream().mapToInt(Integer::intValue).average().orElse(0)));
        vo.setP99LatencyMs(percentile(latencies, 0.99));

        // 近 14 天逐日调用/失败数（缺失日期补 0）
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, long[]> byDay = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 13; i >= 0; i--) {
            byDay.put(today.minusDays(i).format(fmt), new long[]{0, 0});
        }
        for (ToolCallRecord r : records) {
            if (r.getCreatedAt() == null) {
                continue;
            }
            long[] slot = byDay.get(r.getCreatedAt().toLocalDate().format(fmt));
            if (slot != null) {
                slot[0]++;
                if (r.getSuccess() == null || r.getSuccess() != 1) {
                    slot[1]++;
                }
            }
        }
        List<ToolStatsVO.DailyCall> daily = new ArrayList<>();
        for (Map.Entry<String, long[]> e : byDay.entrySet()) {
            ToolStatsVO.DailyCall d = new ToolStatsVO.DailyCall();
            d.setDate(e.getKey());
            d.setCount(e.getValue()[0]);
            d.setFailCount(e.getValue()[1]);
            daily.add(d);
        }
        vo.setDailyCallCount(daily);

        // 调用最多的 Agent TOP5（关联 agent 表取名称）
        Map<Long, Long> byAgent = records.stream()
                .filter(r -> r.getAgentId() != null)
                .collect(Collectors.groupingBy(ToolCallRecord::getAgentId, Collectors.counting()));
        List<ToolStatsVO.AgentCall> topAgents = new ArrayList<>();
        byAgent.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> {
                    ToolStatsVO.AgentCall ac = new ToolStatsVO.AgentCall();
                    ac.setAgentId(e.getKey());
                    Agent agent = agentMapper.selectById(e.getKey());
                    ac.setAgentName(agent != null ? agent.getName() : "未知");
                    ac.setCallCount(e.getValue());
                    topAgents.add(ac);
                });
        vo.setTopAgents(topAgents);
        return vo;
    }

    // ==================== 私有辅助 ====================

    /** 工具全局共享，仅校验存在性（不再校验工作空间归属） */
    private Tool requireToolInWorkspace(Long id) {
        Tool tool = toolMapper.selectById(id);
        if (tool == null) {
            throw new BusinessException(ResultCode.TOOL_NOT_FOUND);
        }
        return tool;
    }

    private ToolSummaryVO toSummaryVO(Tool tool) {
        ToolSummaryVO vo = new ToolSummaryVO();
        vo.setId(tool.getId());
        vo.setName(tool.getName());
        vo.setDisplayName(tool.getDisplayName());
        vo.setDescription(tool.getDescription());
        vo.setCategory(tool.getCategory());
        vo.setCategoryLabel(labelOf(tool.getCategory()));
        vo.setIcon(tool.getIcon());
        vo.setType(tool.getType());
        vo.setStatus(tool.getStatus());
        vo.setBindAgentCount(tool.getBindAgentCount());
        vo.setTotalCalls(tool.getTotalCalls());
        // 成功率：0 调用无数据，返回 null 由前端显示「—」
        vo.setSuccessRate(tool.getTotalCalls() != null && tool.getTotalCalls() > 0
                ? tool.getSuccessRate() : null);
        vo.setAvgLatencyMs(tool.getAvgLatencyMs());
        vo.setCreatedAt(tool.getCreatedAt());
        return vo;
    }

    private ToolVO toDetailVO(Tool tool) {
        ToolVO vo = new ToolVO();
        vo.setId(tool.getId());
        vo.setName(tool.getName());
        vo.setDisplayName(tool.getDisplayName());
        vo.setDescription(tool.getDescription());
        vo.setCategory(tool.getCategory());
        vo.setCategoryLabel(labelOf(tool.getCategory()));
        vo.setIcon(tool.getIcon());
        vo.setType(tool.getType());
        vo.setStatus(tool.getStatus());

        // 扁平 → 嵌套 endpoint
        ToolVO.Endpoint endpoint = new ToolVO.Endpoint();
        endpoint.setUrl(tool.getEndpointUrl());
        endpoint.setMethod(tool.getMethod());
        endpoint.setHeaders(tool.getHeaders());
        endpoint.setTimeoutMs(tool.getEndpointTimeout());
        vo.setEndpoint(endpoint);

        vo.setParameters(tool.getParameters() != null
                ? tool.getParameters() : new ArrayList<Map<String, Object>>());
        vo.setResponseMapping(tool.getResponseMapping());
        vo.setCredentialRef(tool.getCredentialRef());
        vo.setRetryOnFail(toBool(tool.getRetryOnFail()));
        vo.setMaxRetries(tool.getMaxRetries());
        vo.setBindAgentCount(tool.getBindAgentCount());
        vo.setTotalCalls(tool.getTotalCalls());
        // 成功率：0 调用无数据，返回 null 由前端显示「—」
        vo.setSuccessRate(tool.getTotalCalls() != null && tool.getTotalCalls() > 0
                ? tool.getSuccessRate() : null);
        vo.setAvgLatencyMs(tool.getAvgLatencyMs());
        vo.setRecentCalls(loadRecentCalls(tool.getId()));
        vo.setCreatedAt(tool.getCreatedAt());
        return vo;
    }

    /** 最近 10 条调用记录（时间倒序），供详情页"最近调用"页签 */
    private List<ToolVO.ToolCallRecordVO> loadRecentCalls(Long toolId) {
        List<ToolCallRecord> records = toolCallRecordMapper.selectList(
                new LambdaQueryWrapper<ToolCallRecord>()
                        .eq(ToolCallRecord::getToolId, toolId)
                        .orderByDesc(ToolCallRecord::getCreatedAt)
                        .last("LIMIT 10"));
        List<ToolVO.ToolCallRecordVO> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        for (ToolCallRecord r : records) {
            ToolVO.ToolCallRecordVO vo = new ToolVO.ToolCallRecordVO();
            vo.setTime(r.getCreatedAt() != null ? r.getCreatedAt().format(fmt) : null);
            vo.setParams(r.getParams());
            vo.setResultSummary(r.getResultSummary());
            vo.setSuccess(r.getSuccess() != null && r.getSuccess() == 1);
            vo.setLatencyMs(r.getLatencyMs());
            result.add(vo);
        }
        return result;
    }

    private String labelOf(String category) {
        if (category == null) {
            return null;
        }
        return CATEGORY_LABELS.getOrDefault(category, category);
    }

    /** 按工具认证配置追加认证头（bearer / api_key；oauth 依赖外部凭证存储，暂不支持） */
    private void applyAuth(Request.Builder rb, Tool tool) {
        String authType = tool.getAuthType();
        if (!StringUtils.hasText(authType) || "none".equals(authType) || tool.getAuthConfig() == null) {
            return;
        }
        Map<String, Object> cfg = tool.getAuthConfig();
        String value = cfg.get("value") != null ? cfg.get("value").toString()
                : cfg.get("token") != null ? cfg.get("token").toString() : null;
        if ("bearer".equals(authType)) {
            if (StringUtils.hasText(value)) {
                rb.header("Authorization", "Bearer " + value);
            }
        } else if ("api_key".equals(authType)) {
            String header = cfg.get("header") != null ? cfg.get("header").toString()
                    : cfg.get("key") != null ? cfg.get("key").toString() : null;
            if (!StringUtils.hasText(header)) {
                header = "X-API-Key";
            }
            if (StringUtils.hasText(value)) {
                rb.header(header, value);
            }
        }
    }

    /** 简单响应映射：支持 $.a.b.c 与 $.a[0].b 形式的 JSON 路径；未配置或解析失败时返回原始响应体 */
    private String applyMapping(String body, String mapping) {
        if (!StringUtils.hasText(body) || !StringUtils.hasText(mapping)) {
            return body;
        }
        String path = mapping.trim();
        if (path.startsWith("$.")) {
            path = path.substring(2);
        } else if (path.startsWith("$")) {
            path = path.substring(1);
        }
        if (path.isEmpty()) {
            return body;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            for (String seg : path.split("\\.")) {
                if (node == null) {
                    return body;
                }
                if (node.isArray() && seg.matches("\\d+")) {
                    node = node.get(Integer.parseInt(seg));
                } else if (node.isObject()) {
                    node = node.get(seg);
                } else {
                    return body;
                }
            }
            return node == null || node.isNull() ? body : node.toString();
        } catch (Exception e) {
            // 响应非 JSON 或路径不合法：返回原始响应体
            return body;
        }
    }

    /** 测试后回填工具统计（totalCalls/成功率%/平均与P99延迟/健康状态） */
    private void refreshToolStats(Long toolId, boolean lastSuccess) {
        List<ToolCallRecord> records = toolCallRecordMapper.selectList(new LambdaQueryWrapper<ToolCallRecord>()
                .eq(ToolCallRecord::getToolId, toolId));
        long successCount = records.stream()
                .filter(r -> r.getSuccess() != null && r.getSuccess() == 1).count();
        List<Integer> latencies = latenciesOf(records);

        Tool update = new Tool();
        update.setId(toolId);
        update.setTotalCalls((long) records.size());
        update.setSuccessRate(records.isEmpty() ? null
                : BigDecimal.valueOf(successCount * 100.0 / records.size())
                        .setScale(1, BigDecimal.ROUND_HALF_UP));
        update.setAvgLatencyMs(latencies.isEmpty() ? 0
                : (int) Math.round(latencies.stream().mapToInt(Integer::intValue).average().orElse(0)));
        update.setP99LatencyMs(percentile(latencies, 0.99));
        update.setStatus(lastSuccess ? "active" : "error");
        toolMapper.updateById(update);
    }

    /** 成功调用的延迟样本（升序） */
    private List<Integer> latenciesOf(List<ToolCallRecord> records) {
        return records.stream()
                .filter(r -> r.getSuccess() != null && r.getSuccess() == 1 && r.getLatencyMs() != null)
                .map(ToolCallRecord::getLatencyMs)
                .sorted()
                .collect(Collectors.toList());
    }

    /** 有序样本的百分位数（最近邻法；空样本返回 0） */
    private int percentile(List<Integer> sorted, double p) {
        if (sorted.isEmpty()) {
            return 0;
        }
        int idx = (int) Math.ceil(sorted.size() * p) - 1;
        return sorted.get(Math.min(Math.max(idx, 0), sorted.size() - 1));
    }

    /** URL 编码（UTF-8），失败时原样返回 */
    private String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max) : s;
    }

    private Integer toInt(Boolean value) {
        if (value == null) {
            return null;
        }
        return value ? 1 : 0;
    }

    private Boolean toBool(Integer value) {
        if (value == null) {
            return null;
        }
        return value != 0;
    }
}
