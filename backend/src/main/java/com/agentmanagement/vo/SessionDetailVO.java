package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 会话详情 VO（含消息列表，对应前端 getSessionMessages 返回）。
 */
@Data
public class SessionDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long sessionId;

    private String title;

    /** 状态：active/completed/stopped/error */
    private String status;

    private List<MessageVO> messages;
}
