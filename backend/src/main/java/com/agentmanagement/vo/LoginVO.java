package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录响应顶层 data。
 */
@Data
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;

    /** ISO 8601 带时区，如 2026-07-21T10:30:00+08:00（前端 new Date() 可正确解析） */
    private String expiresAt;

    private UserVO user;
}
