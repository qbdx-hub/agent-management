package com.agentmanagement.service.impl;

import com.agentmanagement.common.BusinessException;
import com.agentmanagement.common.PageResult;
import com.agentmanagement.common.ResultCode;
import com.agentmanagement.entity.Agent;
import com.agentmanagement.entity.Message;
import com.agentmanagement.entity.Session;
import com.agentmanagement.form.SendMessageForm;
import com.agentmanagement.form.SessionCreateForm;
import com.agentmanagement.mapper.AgentMapper;
import com.agentmanagement.mapper.MessageMapper;
import com.agentmanagement.mapper.SessionMapper;
import com.agentmanagement.security.SecurityUtils;
import com.agentmanagement.service.SessionService;
import com.agentmanagement.vo.MessageVO;
import com.agentmanagement.vo.SessionDetailVO;
import com.agentmanagement.vo.SessionSummaryVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SessionServiceImpl extends ServiceImpl<SessionMapper, Session> implements SessionService {

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private AgentMapper agentMapper;

    @Override
    @Transactional
    public Long createSession(Long agentId, SessionCreateForm form) {
        Long workspaceId = SecurityUtils.currentWorkspaceId();
        Long userId = SecurityUtils.currentUserId();

        // 校验 Agent 存在且归属当前工作空间
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null || !workspaceId.equals(agent.getWorkspaceId())) {
            throw new BusinessException(ResultCode.AGENT_NOT_FOUND);
        }

        Session session = new Session();
        session.setWorkspaceId(workspaceId);
        session.setAgentId(agentId);
        session.setTitle(form.getTitle() != null ? form.getTitle() : "新会话");
        session.setStatus("active");
        session.setExecutionMode(form.getExecutionMode() != null ? form.getExecutionMode() : "auto");
        if (form.getVariables() != null) {
            Map<String, Object> vars = new HashMap<>();
            vars.putAll(form.getVariables());
            session.setVariables(vars);
        } else {
            session.setVariables(Collections.<String, Object>emptyMap());
        }
        session.setTotalTokens(0L);
        session.setTotalCost(BigDecimal.ZERO);
        session.setMessageCount(0);
        session.setStartedAt(LocalDateTime.now());
        session.setCreatedBy(userId);
        session.setCreatedAt(LocalDateTime.now());

        sessionMapper.insert(session);
        log.info("创建会话: agentId={}, sessionId={}, userId={}", agentId, session.getId(), userId);
        return session.getId();
    }

    @Override
    public PageResult<SessionSummaryVO> pageSessions(Long agentId, int page, int pageSize) {
        Long workspaceId = SecurityUtils.currentWorkspaceId();
        Long userId = SecurityUtils.currentUserId();

        // 校验 Agent 归属
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null || !workspaceId.equals(agent.getWorkspaceId())) {
            throw new BusinessException(ResultCode.AGENT_NOT_FOUND);
        }

        Page<Session> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Session> wrapper = new LambdaQueryWrapper<Session>()
                .eq(Session::getAgentId, agentId)
                .eq(Session::getWorkspaceId, workspaceId)
                .eq(Session::getCreatedBy, userId)
                .orderByDesc(Session::getCreatedAt);

        Page<Session> result = sessionMapper.selectPage(pageParam, wrapper);
        List<SessionSummaryVO> voList = result.getRecords().stream()
                .map(this::toSummaryVO)
                .collect(Collectors.toList());

        return PageResult.of(result, voList);
    }

    @Override
    public SessionDetailVO getSessionMessages(Long sessionId) {
        Long workspaceId = SecurityUtils.currentWorkspaceId();
        Long userId = SecurityUtils.currentUserId();

        Session session = sessionMapper.selectById(sessionId);
        if (session == null || !workspaceId.equals(session.getWorkspaceId())
                || !userId.equals(session.getCreatedBy())) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        // 查询该会话的所有消息
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getSessionId, sessionId)
                .orderByAsc(Message::getCreatedAt);
        List<Message> messages = messageMapper.selectList(wrapper);

        SessionDetailVO vo = new SessionDetailVO();
        vo.setSessionId(session.getId());
        vo.setTitle(session.getTitle());
        vo.setStatus(session.getStatus());
        vo.setMessages(messages.stream().map(this::toMessageVO).collect(Collectors.toList()));

        return vo;
    }

    @Override
    @Transactional
    public Long sendMessage(Long sessionId, SendMessageForm form) {
        Long workspaceId = SecurityUtils.currentWorkspaceId();
        Long userId = SecurityUtils.currentUserId();

        Session session = sessionMapper.selectById(sessionId);
        if (session == null || !workspaceId.equals(session.getWorkspaceId())
                || !userId.equals(session.getCreatedBy())) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        if (!"active".equals(session.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "会话已结束，无法发送消息");
        }

        // 保存用户消息
        Message userMsg = new Message();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(form.getContent());
        userMsg.setMode(form.getMode() != null ? form.getMode() : session.getExecutionMode());
        userMsg.setAttachments(form.getAttachments());
        userMsg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(userMsg);

        // 更新会话统计
        session.setMessageCount(session.getMessageCount() + 1);
        sessionMapper.updateById(session);

        log.info("发送消息: sessionId={}, messageId={}, userId={}", sessionId, userMsg.getId(), userId);
        return userMsg.getId();
    }

    @Override
    public void stopSession(Long sessionId) {
        Long workspaceId = SecurityUtils.currentWorkspaceId();
        Long userId = SecurityUtils.currentUserId();

        Session session = sessionMapper.selectById(sessionId);
        if (session == null || !workspaceId.equals(session.getWorkspaceId())
                || !userId.equals(session.getCreatedBy())) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        session.setStatus("stopped");
        session.setEndedAt(LocalDateTime.now());
        sessionMapper.updateById(session);
        log.info("停止会话: sessionId={}", sessionId);
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId) {
        Long workspaceId = SecurityUtils.currentWorkspaceId();
        Long userId = SecurityUtils.currentUserId();

        Session session = sessionMapper.selectById(sessionId);
        if (session == null || !workspaceId.equals(session.getWorkspaceId())
                || !userId.equals(session.getCreatedBy())) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }

        // 先删消息，再删会话
        LambdaQueryWrapper<Message> msgWrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getSessionId, sessionId);
        messageMapper.delete(msgWrapper);
        sessionMapper.deleteById(sessionId);
        log.info("删除会话: sessionId={}", sessionId);
    }

    // ===== VO 转换 =====

    private SessionSummaryVO toSummaryVO(Session session) {
        SessionSummaryVO vo = new SessionSummaryVO();
        vo.setSessionId(session.getId());
        vo.setTitle(session.getTitle());
        vo.setStatus(session.getStatus());
        vo.setMessageCount(session.getMessageCount());
        vo.setTotalTokens(session.getTotalTokens());
        vo.setTotalCost(session.getTotalCost());
        vo.setCreatedAt(session.getCreatedAt());
        return vo;
    }

    private MessageVO toMessageVO(Message message) {
        MessageVO vo = new MessageVO();
        vo.setMessageId(message.getId());
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setCreatedAt(message.getCreatedAt());

        // token 用量
        if (message.getTokenTotal() != null && message.getTokenTotal() > 0) {
            MessageVO.TokenUsageVO usage = new MessageVO.TokenUsageVO();
            usage.setInput(message.getTokenInput());
            usage.setOutput(message.getTokenOutput());
            usage.setTotal(message.getTokenTotal());
            usage.setCost(message.getTokenCost());
            vo.setTokenUsage(usage);
        }

        return vo;
    }
}
