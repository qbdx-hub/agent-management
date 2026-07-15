package com.agentmanagement.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登录请求表单（对应前端 POST /auth/login 的 {username, password}）。
 */
@Data
public class LoginForm {

    /**
     * 登录账号：可填用户名或邮箱（字段名沿用 username 以兼容前端契约，后端按是否含 @ 分流查询）
     */
    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
