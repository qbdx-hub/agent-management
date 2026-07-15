package com.agentmanagement.service.impl;

import com.agentmanagement.common.BusinessException;
import com.agentmanagement.common.PageResult;
import com.agentmanagement.common.ResultCode;
import com.agentmanagement.entity.Agent;
import com.agentmanagement.entity.AgentPromptVersion;
import com.agentmanagement.entity.AgentToolBinding;
import com.agentmanagement.entity.User;
import com.agentmanagement.form.AgentCreateForm;
import com.agentmanagement.form.AgentQueryForm;
import com.agentmanagement.form.AgentUpdateForm;
import com.agentmanagement.mapper.AgentMapper;
import com.agentmanagement.mapper.AgentPromptVersionMapper;
import com.agentmanagement.mapper.AgentToolBindingMapper;
import com.agentmanagement.mapper.UserMapper;
import com.agentmanagement.security.SecurityUtils;
import com.agentmanagement.service.AgentService;
import com.agentmanagement.vo.AgentSummaryVO;
import com.agentmanagement.vo.AgentVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AgentServiceImpl extends ServiceImpl<AgentMapper, Agent> implements AgentService {

    @Autowired
    private AgentMapper agentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AgentToolBindingMapper agentToolBindingMapper;

    @Autowired
    private AgentPromptVersionMapper agentPromptVersionMapper;

    /** Agent 合法状态机取值 */
    private static final Set<String> VALID_AGENT_STATUS = new HashSet<String>(Arrays.asList(
            "draft", "testing", "published", "paused", "archived"));

    @Override
    public PageResult<AgentSummaryVO> pageAgents(AgentQueryForm form) {
        Long workspaceId = SecurityUtils.currentWorkspaceId();
        Page<Agent> page = new Page<Agent>(form.getPage(), form.getPageSize());
        LambdaQueryWrapper<Agent> qw = new LambdaQueryWrapper<Agent>();
        qw.eq(Agent::getWorkspaceId, workspaceId);
        if (StringUtils.hasText(form.getKeyword())) {
            // keyword 同时匹配 name 与 description
            qw.and(w -> w.like(Agent::getName, form.getKeyword())
                    .or().like(Agent::getDescription, form.getKeyword()));
        }
        qw.eq(StringUtils.hasText(form.getStatus()), Agent::getStatus, form.getStatus());
        // tag 筛选：tags 是 JSON 数组列，用 MySQL JSON_CONTAINS 匹配是否包含该标签（{0} 参数化防注入）
        if (StringUtils.hasText(form.getTag())) {
            qw.apply("JSON_CONTAINS(tags, JSON_QUOTE({0}))", form.getTag());
        }
        qw.orderByDesc(Agent::getCreatedAt);

        Page<Agent> result = agentMapper.selectPage(page, qw);
        Map<Long, String> creatorNameMap = loadUserNames(
                result.getRecords().stream().map(Agent::getCreatedBy).filter(Objects::nonNull).collect(Collectors.toSet()));

        List<AgentSummaryVO> list = new ArrayList<AgentSummaryVO>();
        for (Agent agent : result.getRecords()) {
            AgentSummaryVO vo = new AgentSummaryVO();
            vo.setId(agent.getId());
            vo.setName(agent.getName());
            vo.setDescription(agent.getDescription());
            vo.setAvatar(agent.getAvatar());
            vo.setStatus(agent.getStatus());
            vo.setModelName(agent.getModelName());
            vo.setTags(agent.getTags() != null ? agent.getTags() : new ArrayList<String>());
            vo.setToolCount(0); // 工具绑定子接口未接通，暂固定 0
            vo.setTotalSessions(agent.getTotalSessions());
            vo.setSuccessRate(agent.getSuccessRate());
            vo.setAvgLatencyMs(agent.getAvgLatencyMs());
            vo.setCreatedBy(agent.getCreatedBy());
            vo.setCreatorName(creatorNameMap.get(agent.getCreatedBy()));
            vo.setUpdatedAt(agent.getUpdatedAt());
            list.add(vo);
        }
        return PageResult.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public AgentVO getAgentDetail(Long id) {
        return toDetailVO(requireAgentInWorkspace(id));
    }

    @Override
    public AgentVO createAgent(AgentCreateForm form) {
        Long workspaceId = SecurityUtils.currentWorkspaceId();
        Long currentUserId = SecurityUtils.currentUserId();

        Agent agent = new Agent();
        agent.setWorkspaceId(workspaceId);
        agent.setCreatedBy(currentUserId);
        agent.setName(form.getName());
        agent.setDescription(form.getDescription());
        agent.setAvatar(form.getAvatar());
        agent.setTags(form.getTags());
        agent.setStatus(StringUtils.hasText(form.getStatus()) ? form.getStatus() : "draft");
        // 模型配置（前端创建向导第 2 步采集）
        agent.setModelProvider(form.getModelProvider());
        agent.setModelName(form.getModelName());
        agent.setTemperature(form.getTemperature());
        agent.setMaxTokens(form.getMaxTokens());
        // created_at/updated_at/统计/其余配置走 DB 默认值
        agentMapper.insert(agent);

        // 重新查询以拿到 DB 填充的默认值（temperature/topP/统计等），再组装 VO
        return toDetailVO(agentMapper.selectById(agent.getId()));
    }

    @Override
    public void updateAgent(Long id, AgentUpdateForm form) {
        Agent agent = requireAgentInWorkspace(id);
        Agent update = new Agent();
        update.setId(agent.getId());
        update.setName(form.getName());
        update.setDescription(form.getDescription());
        update.setAvatar(form.getAvatar());
        update.setTags(form.getTags());
        // MyBatis-Plus updateById 默认仅更新非 null 字段，实现部分更新语义
        agentMapper.updateById(update);
    }

    @Override
    public void updateAgentStatus(Long id, String status) {
        if (!StringUtils.hasText(status)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "状态不能为空");
        }
        if (!VALID_AGENT_STATUS.contains(status)) {
            throw new BusinessException(ResultCode.AGENT_STATUS_INVALID);
        }
        Agent agent = requireAgentInWorkspace(id);
        Agent update = new Agent();
        update.setId(agent.getId());
        update.setStatus(status);
        agentMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAgent(Long id) {
        Agent agent = requireAgentInWorkspace(id);
        // 级联清理子表（DB 未定义 ON DELETE CASCADE，否则遗留孤儿绑定/版本数据）
        agentToolBindingMapper.delete(new LambdaQueryWrapper<AgentToolBinding>()
                .eq(AgentToolBinding::getAgentId, agent.getId()));
        agentPromptVersionMapper.delete(new LambdaQueryWrapper<AgentPromptVersion>()
                .eq(AgentPromptVersion::getAgentId, agent.getId()));
        agentMapper.deleteById(agent.getId());
    }

    // ==================== 私有辅助 ====================

    /**
     * 取指定 id 的 Agent，并校验其属于当前工作空间；
     * 不存在或越权一律抛 AGENT_NOT_FOUND（不暴露存在性）。
     */
    private Agent requireAgentInWorkspace(Long id) {
        Agent agent = agentMapper.selectById(id);
        Long workspaceId = SecurityUtils.currentWorkspaceId();
        if (agent == null || !workspaceId.equals(agent.getWorkspaceId())) {
            throw new BusinessException(ResultCode.AGENT_NOT_FOUND);
        }
        return agent;
    }

    /** 扁平 entity → 嵌套 AgentVO（config + stats） */
    private AgentVO toDetailVO(Agent agent) {
        AgentVO vo = new AgentVO();
        vo.setId(agent.getId());
        vo.setName(agent.getName());
        vo.setDescription(agent.getDescription());
        vo.setAvatar(agent.getAvatar());
        vo.setStatus(agent.getStatus());
        vo.setTags(agent.getTags() != null ? agent.getTags() : new ArrayList<String>());
        vo.setCreatedBy(agent.getCreatedBy());
        vo.setCreatorName(loadUserName(agent.getCreatedBy()));
        vo.setCreatedAt(agent.getCreatedAt());
        vo.setUpdatedAt(agent.getUpdatedAt());

        AgentVO.Config config = new AgentVO.Config();
        config.setModelProvider(agent.getModelProvider());
        config.setModelName(agent.getModelName());
        config.setTemperature(agent.getTemperature());
        config.setMaxTokens(agent.getMaxTokens());
        config.setTopP(agent.getTopP());
        config.setSystemPrompt(agent.getSystemPrompt() != null ? agent.getSystemPrompt() : "");
        config.setPromptVariables(agent.getPromptVariables() != null
                ? agent.getPromptVariables() : new ArrayList<Map<String, Object>>());
        config.setBoundTools(new ArrayList<AgentVO.BoundTool>()); // 工具绑定子接口未接通

        AgentVO.Memory memory = new AgentVO.Memory();
        memory.setWorkingWindow(agent.getWorkingWindow());
        memory.setShortTermStrategy(agent.getMemoryStrategy());
        memory.setLongTermEnabled(toBool(agent.getLongTermEnabled()));
        memory.setKnowledgeBaseIds(agent.getKnowledgeBaseIds() != null
                ? agent.getKnowledgeBaseIds() : new ArrayList<Long>());
        config.setMemory(memory);

        AgentVO.Execution execution = new AgentVO.Execution();
        execution.setMaxSteps(agent.getMaxIterations());
        execution.setTimeoutSeconds(agent.getTimeout() == null ? null : agent.getTimeout() / 1000);
        execution.setReflectionEnabled(toBool(agent.getReflectionEnabled()));
        execution.setReflectionDepth(agent.getReflectionDepth());
        execution.setOutputSchema(agent.getOutputSchema());
        config.setExecution(execution);
        vo.setConfig(config);

        AgentVO.Stats stats = new AgentVO.Stats();
        stats.setTotalSessions(agent.getTotalSessions());
        stats.setTotalMessages(agent.getTotalMessages());
        stats.setTotalTokens(agent.getTotalTokens());
        stats.setTotalCost(agent.getTotalCost());
        stats.setSuccessRate(agent.getSuccessRate());
        stats.setAvgLatencyMs(agent.getAvgLatencyMs());
        stats.setAvgStepsPerSession(BigDecimal.ZERO); // 暂无对应统计字段
        vo.setStats(stats);

        return vo;
    }

    private Boolean toBool(Integer value) {
        if (value == null) {
            return null;
        }
        return value != 0;
    }

    private String loadUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        return user == null ? null : user.getUsername();
    }

    private Map<Long, String> loadUserNames(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        return users.stream().collect(Collectors.toMap(User::getId, User::getUsername));
    }
}
