package com.agentmanagement.service.impl;

import com.agentmanagement.common.BusinessException;
import com.agentmanagement.common.ResultCode;
import com.agentmanagement.entity.Role;
import com.agentmanagement.entity.User;
import com.agentmanagement.entity.UserRole;
import com.agentmanagement.entity.Workspace;
import com.agentmanagement.entity.WorkspaceMember;
import com.agentmanagement.form.LoginForm;
import com.agentmanagement.form.RegisterForm;
import com.agentmanagement.mapper.RoleMapper;
import com.agentmanagement.mapper.UserMapper;
import com.agentmanagement.mapper.UserRoleMapper;
import com.agentmanagement.mapper.WorkspaceMapper;
import com.agentmanagement.mapper.WorkspaceMemberMapper;
import com.agentmanagement.security.JwtUtils;
import com.agentmanagement.service.AuthService;
import com.agentmanagement.vo.LoginVO;
import com.agentmanagement.vo.UserVO;
import com.agentmanagement.vo.WorkspaceBriefVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private WorkspaceMemberMapper workspaceMemberMapper;
    @Autowired
    private WorkspaceMapper workspaceMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtils jwtUtils;

    /** role.name → 优先级（值越小优先级越高），不依赖 role.id 排序 */
    private static final Map<String, Integer> ROLE_PRIORITY = new HashMap<String, Integer>();
    static {
        ROLE_PRIORITY.put("super_admin", 0);
        ROLE_PRIORITY.put("admin", 1);
        ROLE_PRIORITY.put("developer", 2);
        ROLE_PRIORITY.put("observer", 3);
    }

    /** 默认工作空间：注册即加入（对应种子数据 id=1「默认工作空间」） */
    private static final Long DEFAULT_WORKSPACE_ID = 1L;
    /** 新注册用户默认系统角色：只读 observer */
    private static final String DEFAULT_ROLE_NAME = "observer";

    @Override
    public LoginVO login(LoginForm form) {
        // 1. 按用户名或邮箱查用户：含 @ 视为按邮箱查（username 正则 [a-zA-Z0-9_-] 不含 @，二者互斥）
        String account = form.getUsername();
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>();
        if (account.contains("@")) {
            queryWrapper.eq(User::getEmail, account);
        } else {
            queryWrapper.eq(User::getUsername, account);
        }
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            // 用户不存在与密码错用同一文案，防止用户名枚举
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }

        // 2. BCrypt 校验
        if (!passwordEncoder.matches(form.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }

        // 3. 状态检查（status != 1 视为禁用，防御 null）
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        // 4. 组装 role + permissions
        Role primaryRole = loadPrimaryRole(user.getId());
        List<String> permissions = (primaryRole != null && primaryRole.getPermissions() != null)
                ? primaryRole.getPermissions() : Collections.<String>emptyList();
        String roleCode = mapRoleNameToCode(primaryRole);

        // 5. 组装 workspaces
        List<WorkspaceBriefVO> workspaces = loadWorkspaces(user.getId());

        // 6. 更新最后登录时间
        User update = new User();
        update.setId(user.getId());
        update.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(update);

        // 7. 签发 token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        Date expiration = jwtUtils.getExpirationFromToken(token);

        // 8. 组装返回 VO
        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setNickname(user.getNickname());
        userVO.setAvatar(user.getAvatar() == null ? "" : user.getAvatar());
        userVO.setEmail(user.getEmail());
        userVO.setRole(roleCode);
        userVO.setPermissions(permissions);
        userVO.setWorkspaces(workspaces);

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setExpiresAt(formatIso8601(expiration));
        vo.setUser(userVO);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterForm form) {
        // 1. 用户名查重（user 表 uk_username 唯一约束兜底并发撞键，由 GlobalExceptionHandler 映射为 1006）
        Long usernameCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, form.getUsername()));
        if (usernameCount != null && usernameCount > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        // 2. 邮箱查重（user 表 uk_email 唯一约束兜底并发撞键，由 GlobalExceptionHandler 映射为 1007）
        Long emailCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, form.getEmail()));
        if (emailCount != null && emailCount > 0) {
            throw new BusinessException(ResultCode.EMAIL_EXISTS);
        }

        // 3. 写入用户：密码 BCrypt 加密，status=1 启用；created_at/updated_at 由 DB 默认填充
        User user = new User();
        user.setUsername(form.getUsername());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setEmail(form.getEmail());
        user.setNickname(form.getNickname());
        user.setStatus(1);
        userMapper.insert(user);

        // 4. 分配默认系统角色 observer（按 name+系统角色查，避免硬编码 role.id）
        Role observerRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>()
                        .eq(Role::getName, DEFAULT_ROLE_NAME)
                        .isNull(Role::getWorkspaceId));
        if (observerRole != null) {
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(observerRole.getId());
            userRole.setWorkspaceId(DEFAULT_WORKSPACE_ID);
            userRoleMapper.insert(userRole);
        } else {
            // 角色缺失视为配置异常：不阻断注册（降级为无角色），但必须留日志便于排查
            log.warn("默认系统角色 observer 未找到，用户 id={} 暂未分配角色，请检查 role 种子数据", user.getId());
        }

        // 5. 加入默认工作空间前校验其存在，避免种子缺失时写入指向不存在工作空间的幽灵成员
        Workspace defaultWorkspace = workspaceMapper.selectById(DEFAULT_WORKSPACE_ID);
        if (defaultWorkspace == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "默认工作空间未初始化，请联系管理员");
        }
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(DEFAULT_WORKSPACE_ID);
        member.setUserId(user.getId());
        member.setRole("member");
        // joined_at 由 DB 默认填充
        workspaceMemberMapper.insert(member);
    }

    /** 取用户最高优先级的角色 */
    private Role loadPrimaryRole(Long userId) {
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return null;
        }
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        if (roles.isEmpty()) {
            return null;
        }
        return roles.stream()
                .min(Comparator.comparingInt(r -> rolePriority(r.getName())))
                .orElse(null);
    }

    private int rolePriority(String name) {
        return ROLE_PRIORITY.getOrDefault(name, 99);
    }

    /** role.name → 前端 role 大写：super_admin/admin→ADMIN，developer→DEVELOPER，observer→VIEWER */
    private String mapRoleNameToCode(Role role) {
        if (role == null || role.getName() == null) {
            return "VIEWER";
        }
        switch (role.getName()) {
            case "super_admin":
            case "admin":
                return "ADMIN";
            case "developer":
                return "DEVELOPER";
            case "observer":
                return "VIEWER";
            default:
                return "VIEWER";
        }
    }

    /** 查用户所在工作空间列表（返回 DB 原始 role：owner/admin/member） */
    private List<WorkspaceBriefVO> loadWorkspaces(Long userId) {
        List<WorkspaceMember> members = workspaceMemberMapper.selectList(
                new LambdaQueryWrapper<WorkspaceMember>().eq(WorkspaceMember::getUserId, userId));
        if (members.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> wsIds = members.stream()
                .map(WorkspaceMember::getWorkspaceId)
                .distinct()
                .collect(Collectors.toList());
        List<Workspace> workspaces = workspaceMapper.selectBatchIds(wsIds);
        Map<Long, String> wsNameMap = workspaces.stream()
                .collect(Collectors.toMap(Workspace::getId, Workspace::getName));

        List<WorkspaceBriefVO> result = new ArrayList<WorkspaceBriefVO>();
        for (WorkspaceMember m : members) {
            WorkspaceBriefVO brief = new WorkspaceBriefVO();
            brief.setId(m.getWorkspaceId());
            brief.setName(wsNameMap.get(m.getWorkspaceId()));
            brief.setRole(m.getRole());
            result.add(brief);
        }
        return result;
    }

    /** 格式化为 ISO 8601 带时区：2026-07-21T10:30:00+08:00（前端 new Date() 可解析） */
    private String formatIso8601(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        return sdf.format(date);
    }
}
