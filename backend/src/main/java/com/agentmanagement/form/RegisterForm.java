package com.agentmanagement.form;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 注册请求表单（对应前端 POST /auth/register 的 {username, nickname, email, password}）。
 * confirmPassword 仅前端校验，后端不接收。
 */
@Data
public class RegisterForm {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,50}$", message = "用户名只能包含字母、数字、下划线、短横线，长度3-50")
    private String username;

    @NotBlank(message = "昵称不能为空")
    @Pattern(regexp = "^[^\\u0000-\\u001F]{1,50}$", message = "昵称长度1-50且不能含控制字符")
    private String nickname;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z0-9!@#$%^&*._-]{8,50}$", message = "密码需8-50位且包含字母和数字")
    private String password;
}
