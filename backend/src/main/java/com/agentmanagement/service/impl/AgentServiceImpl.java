package com.agentmanagement.service.impl;

import com.agentmanagement.common.BusinessException;
import com.agentmanagement.common.PageResult;
import com.agentmanagement.common.ResultCode;
import com.agentmanagement.entity.Agent;
import com.agentmanagement.entity.AgentPromptVersion;
import com.agentmanagement.entity.AgentToolBinding;
import com.agentmanagement.entity.KnowledgeBase;
import com.agentmanagement.entity.Tool;
import com.agentmanagement.entity.User;
import com.agentmanagement.form.AgentCreateForm;
import com.agentmanagement.form.AgentQueryForm;
import com.agentmanagement.form.AgentUpdateForm;
import com.agentmanagement.mapper.AgentMapper;
import com.agentmanagement.mapper.AgentPromptVersionMapper;
import com.agentmanagement.mapper.AgentToolBindingMapper;
import com.agentmanagement.mapper.KnowledgeBaseMapper;
import com.agentmanagement.mapper.ToolMapper;
import com.agentmanagement.mapper.UserMapper;
import com.agentmanagement.security.SecurityUtils;
import com.agentmanagement.security.UserRoleChecker;
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
import java.time.LocalDateTime;
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

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private ToolMapper toolMapper;

    @Autowired
    private UserRoleChecker userRoleChecker;

    /** Agent 合法状态机取值 */
    private static final Set<String> VALID_AGENT_STATUS = new HashSet<String>(Arrays.asList(
            "draft", "testing", "published", "paused", "archived"));

    @Override
    public PageResult<AgentSummaryVO> pageAgents(AgentQueryForm form) {
        Long workspaceId = SecurityUtils.currentWorkspaceId();
        Long currentUserId = SecurityUtils.currentUserId();
        Page<Agent> page = new Page<Agent>(form.getPage(), form.getPageSize());
        LambdaQueryWrapper<Agent> qw = new LambdaQueryWrapper<Agent>();
        qw.eq(Agent::getWorkspaceId, workspaceId);
        // 按账户隔离：当前用户创建的 + 历史未设置 createdBy 的
        qw.and(w -> w.eq(Agent::getCreatedBy, currentUserId)
                .or().isNull(Agent::getCreatedBy));
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
        // 批量统计本页各 Agent 启用中的工具绑定数，避免 N+1 查询
        Map<Long, Long> toolCountMap = countEnabledBindings(
                result.getRecords().stream().map(Agent::getId).collect(Collectors.toSet()));

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
            vo.setToolCount(toolCountMap.getOrDefault(agent.getId(), 0L).intValue());
            vo.setTotalSessions(agent.getTotalSessions());
            // 成功率：0 会话无数据，返回 null 由前端显示「—」（历史默认值 100 不可信）
            vo.setSuccessRate(agent.getTotalSessions() != null && agent.getTotalSessions() > 0
                    ? agent.getSuccessRate() : null);
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
    @Transactional(rollbackFor = Exception.class)
    public void updateAgent(Long id, AgentUpdateForm form) {
        Agent agent = requireAgentInWorkspace(id);
        Long workspaceId = SecurityUtils.currentWorkspaceId();
        Long currentUserId = SecurityUtils.currentUserId();
        // 知识库绑定隔离：只允许绑定当前用户可见的库（admin 全部/本人创建/历史 NULL）
        checkKnowledgeBaseAccess(form.getKnowledgeBaseIds(), currentUserId, workspaceId);
        Agent update = new Agent();
        update.setId(agent.getId());
        // 基本信息
        update.setName(form.getName());
        update.setDescription(form.getDescription());
        update.setAvatar(form.getAvatar());
        update.setTags(form.getTags());
        // AI 连接配置
        update.setAiBaseUrl(form.getAiBaseUrl());
        update.setAiApiKey(form.getAiApiKey());
        update.setAiModel(form.getAiModel());
        // 模型配置
        update.setModelProvider(form.getModelProvider());
        update.setModelName(form.getModelName());
        update.setTemperature(form.getTemperature());
        update.setMaxTokens(form.getMaxTokens());
        update.setTopP(form.getTopP());
        // 提示词配置
        update.setSystemPrompt(form.getSystemPrompt());
        update.setPromptVariables(form.getPromptVariables());
        // 记忆配置
        update.setMemoryStrategy(form.getMemoryStrategy());
        update.setWorkingWindow(form.getWorkingWindow());
        update.setLongTermEnabled(form.getLongTermEnabled());
        update.setKnowledgeBaseIds(form.getKnowledgeBaseIds());
        // Token 价格配置
        update.setInputPricePerMillion(form.getInputPricePerMillion());
        update.setCachedInputPricePerMillion(form.getCachedInputPricePerMillion());
        update.setOutputPricePerMillion(form.getOutputPricePerMillion());
        // 执行配置
        update.setMaxIterations(form.getMaxIterations());
        update.setTimeout(form.getTimeout());
        update.setReflectionEnabled(form.getReflectionEnabled());
        update.setReflectionDepth(form.getReflectionDepth());
        update.setOutputSchema(form.getOutputSchema());
        // MyBatis-Plus updateById 默认仅更新非 null 字段，实现部分更新语义。
        // 全 null 时会生成无 SET 子句的 UPDATE agent WHERE id=?，被 Druid wall 拒绝 → 仅在
        // 至少一个标量字段非 null 时才执行（工具绑定在下方独立处理，不依赖这次 update）。
        boolean hasScalar = update.getName() != null || update.getDescription() != null
                || update.getAvatar() != null || update.getTags() != null
                || update.getAiBaseUrl() != null || update.getAiApiKey() != null
                || update.getAiModel() != null || update.getModelProvider() != null
                || update.getModelName() != null || update.getTemperature() != null
                || update.getMaxTokens() != null || update.getTopP() != null
                || update.getSystemPrompt() != null || update.getPromptVariables() != null
                || update.getMemoryStrategy() != null || update.getWorkingWindow() != null
                || update.getLongTermEnabled() != null || update.getKnowledgeBaseIds() != null
                || update.getInputPricePerMillion() != null
                || update.getCachedInputPricePerMillion() != null
                || update.getOutputPricePerMillion() != null
                || update.getMaxIterations() != null || update.getTimeout() != null
                || update.getReflectionEnabled() != null || update.getReflectionDepth() != null
                || update.getOutputSchema() != null;
        if (hasScalar) {
            agentMapper.updateById(update);
        }

        // 工具绑定：前端全量提交（所有工具 + enabled 标记），null 表示本次不改绑定
        if (form.getToolBindings() != null) {
            replaceToolBindings(agent.getId(), form.getToolBindings(), workspaceId);
        }
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

    /** 校验待绑定的知识库全部对当前用户可见：须属于当前工作空间，admin 全部可见，普通用户仅本人创建或历史 NULL */
    private void checkKnowledgeBaseAccess(List<Long> kbIds, Long userId, Long workspaceId) {
        if (kbIds == null || kbIds.isEmpty()) {
            return;
        }
        boolean admin = userRoleChecker.isAdmin(userId);
        for (Long kbId : kbIds) {
            KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
            boolean visible = kb != null && workspaceId.equals(kb.getWorkspaceId())
                    && (admin || kb.getCreatedBy() == null || kb.getCreatedBy().equals(userId));
            if (!visible) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "知识库不存在或无权限绑定: " + kbId);
            }
        }
    }

    /**
     * 全量替换 Agent 的工具绑定：先删后插（绑定数量少，代价可忽略），
     * 只落 enabled=true 的绑定，并回填受影响工具的 bind_agent_count 统计。
     */
    private void replaceToolBindings(Long agentId, List<AgentUpdateForm.ToolBindingItem> items, Long workspaceId) {
        // 旧绑定涉及的工具也要回填计数（解绑后计数需下降）
        Set<Long> affectedToolIds = agentToolBindingMapper.selectList(new LambdaQueryWrapper<AgentToolBinding>()
                        .eq(AgentToolBinding::getAgentId, agentId))
                .stream().map(AgentToolBinding::getToolId).collect(Collectors.toSet());

        // 校验工具属于当前工作空间；按 toolId 去重
        Set<Long> enabledToolIds = new HashSet<>();
        List<AgentToolBinding> toInsert = new ArrayList<>();
        for (AgentUpdateForm.ToolBindingItem item : items) {
            if (item == null || item.getToolId() == null || !Boolean.TRUE.equals(item.getEnabled())) {
                continue;
            }
            if (!enabledToolIds.add(item.getToolId())) {
                continue;
            }
            Tool tool = toolMapper.selectById(item.getToolId());
            // 工具市场全局共享：任意账户可绑定任意工具，仅校验存在性
            if (tool == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "工具不存在: " + item.getToolId());
            }
            AgentToolBinding binding = new AgentToolBinding();
            binding.setAgentId(agentId);
            binding.setToolId(item.getToolId());
            binding.setEnabled(1);
            binding.setCreatedAt(LocalDateTime.now());
            toInsert.add(binding);
        }

        agentToolBindingMapper.delete(new LambdaQueryWrapper<AgentToolBinding>()
                .eq(AgentToolBinding::getAgentId, agentId));
        for (AgentToolBinding binding : toInsert) {
            agentToolBindingMapper.insert(binding);
        }

        affectedToolIds.addAll(enabledToolIds);
        refreshToolBindCount(affectedToolIds);
    }

    /** 回填工具的 bind_agent_count（enabled=1 的绑定数） */
    private void refreshToolBindCount(Collection<Long> toolIds) {
        for (Long toolId : toolIds) {
            Long count = agentToolBindingMapper.selectCount(new LambdaQueryWrapper<AgentToolBinding>()
                    .eq(AgentToolBinding::getToolId, toolId)
                    .eq(AgentToolBinding::getEnabled, 1));
            Tool update = new Tool();
            update.setId(toolId);
            update.setBindAgentCount(count.intValue());
            toolMapper.updateById(update);
        }
    }

    /** 加载 Agent 的全部绑定（含未启用），关联 tool 表取名称/图标；工具已删除的脏绑定跳过 */
    private List<AgentVO.BoundTool> loadBoundTools(Long agentId) {
        List<AgentToolBinding> bindings = agentToolBindingMapper.selectList(
                new LambdaQueryWrapper<AgentToolBinding>()
                        .eq(AgentToolBinding::getAgentId, agentId)
                        .orderByAsc(AgentToolBinding::getCreatedAt));
        if (bindings.isEmpty()) {
            return new ArrayList<AgentVO.BoundTool>();
        }
        List<Long> toolIds = bindings.stream().map(AgentToolBinding::getToolId).collect(Collectors.toList());
        Map<Long, Tool> toolMap = toolMapper.selectBatchIds(toolIds).stream()
                .collect(Collectors.toMap(Tool::getId, t -> t));
        List<AgentVO.BoundTool> result = new ArrayList<>();
        for (AgentToolBinding binding : bindings) {
            Tool tool = toolMap.get(binding.getToolId());
            if (tool == null) {
                continue;
            }
            AgentVO.BoundTool bt = new AgentVO.BoundTool();
            bt.setToolId(tool.getId());
            bt.setToolName(tool.getDisplayName() != null ? tool.getDisplayName() : tool.getName());
            bt.setToolIcon(tool.getIcon());
            bt.setEnabled(binding.getEnabled() != null && binding.getEnabled() == 1);
            result.add(bt);
        }
        return result;
    }

    /** 统计各 Agent 启用中的工具绑定数 */
    private Map<Long, Long> countEnabledBindings(Collection<Long> agentIds) {
        if (agentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<AgentToolBinding> bindings = agentToolBindingMapper.selectList(
                new LambdaQueryWrapper<AgentToolBinding>()
                        .in(AgentToolBinding::getAgentId, agentIds)
                        .eq(AgentToolBinding::getEnabled, 1));
        return bindings.stream().collect(Collectors.groupingBy(AgentToolBinding::getAgentId, Collectors.counting()));
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
        // AI 连接配置（脱敏）
        vo.setAiBaseUrl(agent.getAiBaseUrl());
        vo.setAiApiKeyMasked(maskApiKey(agent.getAiApiKey()));
        vo.setAiModel(agent.getAiModel());
        // Token 价格配置
        vo.setInputPricePerMillion(agent.getInputPricePerMillion());
        vo.setCachedInputPricePerMillion(agent.getCachedInputPricePerMillion());
        vo.setOutputPricePerMillion(agent.getOutputPricePerMillion());

        AgentVO.Config config = new AgentVO.Config();
        config.setModelProvider(agent.getModelProvider());
        config.setModelName(agent.getModelName());
        config.setTemperature(agent.getTemperature());
        config.setMaxTokens(agent.getMaxTokens());
        config.setTopP(agent.getTopP());
        config.setSystemPrompt(agent.getSystemPrompt() != null ? agent.getSystemPrompt() : "");
        config.setPromptVariables(agent.getPromptVariables() != null
                ? agent.getPromptVariables() : new ArrayList<Map<String, Object>>());
        config.setBoundTools(loadBoundTools(agent.getId()));

        AgentVO.Memory memory = new AgentVO.Memory();
        memory.setWorkingWindow(agent.getWorkingWindow());
        memory.setShortTermStrategy(agent.getMemoryStrategy());
        memory.setLongTermEnabled(toBool(agent.getLongTermEnabled()));
        if (agent.getKnowledgeBaseIds() != null) {
            List<Long> kbIds = new ArrayList<>();
            for (Object id : agent.getKnowledgeBaseIds()) {
                kbIds.add(id instanceof Number ? ((Number) id).longValue() : Long.valueOf(id.toString()));
            }
            memory.setKnowledgeBaseIds(kbIds);
        } else {
            memory.setKnowledgeBaseIds(new ArrayList<Long>());
        }
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
        stats.setSuccessRate(agent.getTotalSessions() != null && agent.getTotalSessions() > 0
                ? agent.getSuccessRate() : null);
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

    /** API Key 脱敏：只显示前 8 位 + *** */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return apiKey;
        }
        return apiKey.substring(0, 8) + "***";
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
