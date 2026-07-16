package com.agentmanagement.controller;

import com.agentmanagement.common.Result;
import com.agentmanagement.form.LoginForm;
import com.agentmanagement.form.RegisterForm;
import com.agentmanagement.form.UserProfileForm;
import com.agentmanagement.service.AuthService;
import com.agentmanagement.vo.LoginVO;
import com.agentmanagement.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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

    /** GET /api/v1/auth/me —— 获取当前登录用户信息 */
    @GetMapping("/me")
    public Result<UserVO> getCurrentUser() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Result.success(authService.getCurrentUser(userId));
    }

    /** PUT /api/v1/auth/profile —— 修改当前用户个人信息（用户名、昵称、邮箱、密码） */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UserProfileForm form) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        authService.updateProfile(userId, form);
        return Result.success();
    }
}
