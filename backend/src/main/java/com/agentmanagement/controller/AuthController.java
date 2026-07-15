package com.agentmanagement.controller;

import com.agentmanagement.common.Result;
import com.agentmanagement.form.LoginForm;
import com.agentmanagement.form.RegisterForm;
import com.agentmanagement.service.AuthService;
import com.agentmanagement.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 认证接口（前缀 /api/v1 由 context-path 统一加）。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /** POST /api/v1/auth/login —— 用户名密码登录 */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginForm form) {
        return Result.success(authService.login(form));
    }

    /** POST /api/v1/auth/register —— 用户注册，成功无返回体 */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterForm form) {
        authService.register(form);
        return Result.success();
    }
}
