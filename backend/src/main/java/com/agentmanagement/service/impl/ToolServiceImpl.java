package com.agentmanagement.service.impl;

import com.agentmanagement.common.BusinessException;
import com.agentmanagement.common.PageResult;
import com.agentmanagement.common.ResultCode;
import com.agentmanagement.entity.Tool;
import com.agentmanagement.form.ToolQueryForm;
import com.agentmanagement.form.ToolRegisterForm;
import com.agentmanagement.form.ToolUpdateForm;
import com.agentmanagement.mapper.ToolMapper;
import com.agentmanagement.security.SecurityUtils;
import com.agentmanagement.service.ToolService;
import com.agentmanagement.vo.ToolSummaryVO;
import com.agentmanagement.vo.ToolVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ToolServiceImpl extends ServiceImpl<ToolMapper, Tool> implements ToolService {

    @Autowired
    private ToolMapper toolMapper;

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
        Long workspaceId = SecurityUtils.currentWorkspaceId();
        Page<Tool> page = new Page<Tool>(form.getPage(), form.getPageSize());
        LambdaQueryWrapper<Tool> qw = new LambdaQueryWrapper<Tool>();
        qw.eq(Tool::getWorkspaceId, workspaceId);
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

    // ==================== 私有辅助 ====================

    private Tool requireToolInWorkspace(Long id) {
        Tool tool = toolMapper.selectById(id);
        Long workspaceId = SecurityUtils.currentWorkspaceId();
        if (tool == null || !workspaceId.equals(tool.getWorkspaceId())) {
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
        vo.setSuccessRate(tool.getSuccessRate());
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
        vo.setSuccessRate(tool.getSuccessRate());
        vo.setAvgLatencyMs(tool.getAvgLatencyMs());
        vo.setRecentCalls(new ArrayList<ToolVO.ToolCallRecordVO>()); // 调用记录由监控模块补
        vo.setCreatedAt(tool.getCreatedAt());
        return vo;
    }

    private String labelOf(String category) {
        if (category == null) {
            return null;
        }
        return CATEGORY_LABELS.getOrDefault(category, category);
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
