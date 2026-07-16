package com.agentmanagement.service;

import com.agentmanagement.common.PageResult;
import com.agentmanagement.form.SendMessageForm;
import com.agentmanagement.form.SessionCreateForm;
import com.agentmanagement.entity.Session;
import com.agentmanagement.vo.MessageVO;
import com.agentmanagement.vo.SessionDetailVO;
import com.agentmanagement.vo.SessionSummaryVO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface SessionService extends IService<Session> {

    /**
     * 创建会话
     * @param agentId Agent ID
     * @param form 创建表单
     * @return 新会话 ID
     */
    Long createSession(Long agentId, SessionCreateForm form);

    /**
     * 分页查询 Agent 的会话列表
     */
    PageResult<SessionSummaryVO> pageSessions(Long agentId, int page, int pageSize);

    /**
     * 获取会话详情（含消息列表）
     */
    SessionDetailVO getSessionMessages(Long sessionId);

    /**
     * 发送用户消息，返回 messageId
     */
    Long sendMessage(Long sessionId, SendMessageForm form);

    /**
     * 停止会话
     */
    void stopSession(Long sessionId);

    /**
     * 删除会话
     */
    void deleteSession(Long sessionId);
}
