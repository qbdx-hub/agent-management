package com.agentmanagement.security;

import com.agentmanagement.entity.Role;
import com.agentmanagement.entity.UserRole;
import com.agentmanagement.mapper.RoleMapper;
import com.agentmanagement.mapper.UserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户角色判断工具：查 user_role + role 表判断当前用户是否系统管理员。
 * 角色名对齐 AuthServiceImpl#mapRoleNameToCode：super_admin/admin 均视为 ADMIN。
 */
@Component
public class UserRoleChecker {

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RoleMapper roleMapper;

    /**
     * 判断用户是否持有系统管理员角色（super_admin / admin）。
     */
    public boolean isAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (userRoles.isEmpty()) {
            return false;
        }
        List<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId).collect(Collectors.toList());
        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        return roles.stream().anyMatch(r ->
                "super_admin".equals(r.getName()) || "admin".equals(r.getName()));
    }
}
