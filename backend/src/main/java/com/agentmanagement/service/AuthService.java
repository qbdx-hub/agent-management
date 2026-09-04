package com.agentmanagement.service;

import com.agentmanagement.form.LoginForm;
import com.agentmanagement.form.RegisterForm;
import com.agentmanagement.form.UserProfileForm;
import com.agentmanagement.vo.LoginVO;
import com.agentmanagement.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 认证服务（登录、注册、当前用户信息、个人信息修改）。
 * auth 无独立 entity 的 CRUD，不继承 IService。
 */
public interface AuthService {

    /**
     * 用户名密码登录：校验 user 表 → 签发 JWT → 组装角色/权限/工作空间。
     */
    LoginVO login(LoginForm form);

    /**
     * 用户注册：校验用户名/邮箱唯一 → BCrypt 加密 → 写 user 表
     * → 分配默认系统角色(observer) → 加入默认工作空间(member)。
     * 整体事务，任一失败回滚。
     */
    void register(RegisterForm form);

    /**
     * 获取当前登录用户信息（与登录返回的 user 结构一致，不含 token）。
     */
    UserVO getCurrentUser(Long userId);

    /**
     * 修改当前登录用户的个人信息（用户名、昵称、邮箱、密码）。
     * 密码修改需先校验旧密码。
     */
    void updateProfile(Long userId, UserProfileForm form);

    /**
     * 上传当前用户头像（multipart，≤2MB，jpg/png/webp/gif），
     * 覆盖式存储到 {file.upload-dir}/avatars/，返回带版本参数的可访问 URL。
     */
    String uploadAvatar(Long userId, MultipartFile file);

    /**
     * 读取当前用户偏好（user.preferences JSON）。
     * 未设置时返回移动端约定的默认结构（模型/温度/通知等）。
     */
    Map<String, Object> getPreferences(Long userId);

    /**
     * 全量覆盖保存用户偏好（JSON 透传，结构由前端约定）。
     */
    void updatePreferences(Long userId, Map<String, Object> preferences);
}
