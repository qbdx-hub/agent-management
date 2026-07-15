package com.agentmanagement.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登录请求表单（对应前端 POST /auth/login 的 {username, password}）。
 */
@Data
public class LoginForm {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
