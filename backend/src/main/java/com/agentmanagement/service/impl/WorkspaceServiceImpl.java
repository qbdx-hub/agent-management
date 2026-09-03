package com.agentmanagement.service.impl;

import com.agentmanagement.common.BusinessException;
import com.agentmanagement.common.PageResult;
import com.agentmanagement.common.ResultCode;
import com.agentmanagement.entity.ActivityLog;
import com.agentmanagement.entity.Agent;
import com.agentmanagement.entity.Session;
import com.agentmanagement.entity.User;
import com.agentmanagement.entity.Workspace;
import com.agentmanagement.entity.WorkspaceMember;
import com.agentmanagement.mapper.ActivityLogMapper;
import com.agentmanagement.mapper.AgentMapper;
import com.agentmanagement.mapper.SessionMapper;
import com.agentmanagement.mapper.UserMapper;
import com.agentmanagement.mapper.WorkspaceMapper;
import com.agentmanagement.mapper.WorkspaceMemberMapper;
import com.agentmanagement.security.SecurityUtils;
import com.agentmanagement.form.MemberInviteForm;
import com.agentmanagement.form.WorkspaceCreateForm;
import com.agentmanagement.form.WorkspaceSettingsForm;
import com.agentmanagement.service.WorkspaceService;
import com.agentmanagement.vo.WorkspaceMemberVO;
import com.agentmanagement.vo.WorkspaceSettingsVO;
import com.agentmanagement.vo.WorkspaceVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工作空间服务实现。
 * 个人使用场景下的最小实现：邀请即直接加入空间（不发邮件）；
 * 成员角色沿用前端词表 owner/ADMIN/MANAGER/DEVELOPER/VIEWER。
 * 活动流由 AuditLogAspect 在每次 @AuditLog 操作成功后统一写入 activity_log。
 */
@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private WorkspaceMapper workspaceMapper;

    @Autowired
    private WorkspaceMemberMapper workspaceMemberMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AgentMapper agentMapper;

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private ActivityLogMapper activityLogMapper;

    private static final Map<String, String> ROLE_LABELS = Collections.unmodifiableMap(new java.util.HashMap<String, String>() {{
        put("owner", "所有者");
        put("ADMIN", "管理员");
        put("MANAGER", "管理者");
        put("DEVELOPER", "开发者");
        put("VIEWER", "只读");
    }});

    @Override
    public List<WorkspaceVO> myWorkspaces() {
        Long userId = SecurityUtils.currentUserId();
        List<WorkspaceMember> memberships = workspaceMemberMapper.selectList(
                new LambdaQueryWrapper<WorkspaceMember>().eq(WorkspaceMember::getUserId, userId));
        if (memberships.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = memberships.stream().map(WorkspaceMember::getWorkspaceId).collect(Collectors.toList());
        Map<Long, Workspace> wsMap = workspaceMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Workspace::getId, Function.identity()));
        Map<Long, String> roleMap = memberships.stream()
                .collect(Collectors.toMap(WorkspaceMember::getWorkspaceId, m -> m.getRole(), (a, b) -> a));

        List<WorkspaceVO> list = new ArrayList<>();
        for (Workspace ws : wsMap.values()) {
            WorkspaceVO vo = new WorkspaceVO();
            vo.setId(ws.getId());
            vo.setName(ws.getName());
            vo.setDescription(ws.getDescription());
            vo.setRole(roleMap.get(ws.getId()));
            vo.setMemberCount(workspaceMemberMapper.selectCount(
                    new LambdaQueryWrapper<WorkspaceMember>().eq(WorkspaceMember::getWorkspaceId, ws.getId())));
            vo.setAgentCount(agentMapper.selectCount(
                    new LambdaQueryWrapper<Agent>().eq(Agent::getWorkspaceId, ws.getId())));
            vo.setCreatedAt(ws.getCreatedAt());
            list.add(vo);
        }
        list.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkspaceVO create(WorkspaceCreateForm form) {
        Long userId = SecurityUtils.currentUserId();
        Workspace ws = new Workspace();
        ws.setName(form.getName());
        ws.setDescription(form.getDescription());
        ws.setOwnerId(userId);
        ws.setStatus(1);
        // 运行配置默认值（与空间设置页可改字段一致）
        ws.setDefaultModelProvider("openai");
        ws.setSessionRetentionDays(90);
        ws.setAutoArchiveDays(30);
        ws.setMaxTokensPerTask(100000L);
        ws.setLanguage("zh-CN");
        ws.setCreatedAt(LocalDateTime.now());
        ws.setUpdatedAt(LocalDateTime.now());
        workspaceMapper.insert(ws);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(ws.getId());
        member.setUserId(userId);
        member.setRole("owner");
        member.setJoinedAt(LocalDateTime.now());
        member.setLastActiveAt(LocalDateTime.now());
        workspaceMemberMapper.insert(member);

        WorkspaceVO vo = new WorkspaceVO();
        vo.setId(ws.getId());
        vo.setName(ws.getName());
        vo.setDescription(ws.getDescription());
        vo.setRole("owner");
        vo.setMemberCount(1L);
        vo.setAgentCount(0L);
        vo.setCreatedAt(ws.getCreatedAt());
        return vo;
    }

    @Override
    public WorkspaceSettingsVO getSettings(Long workspaceId) {
        Workspace ws = requireMember(workspaceId);
        WorkspaceSettingsVO vo = new WorkspaceSettingsVO();
        vo.setDefaultModelProvider(ws.getDefaultModelProvider());
        vo.setSessionRetentionDays(ws.getSessionRetentionDays());
        vo.setAutoArchiveDays(ws.getAutoArchiveDays());
        vo.setMaxTokensPerTask(ws.getMaxTokensPerTask());
        vo.setLanguage(ws.getLanguage());
        return vo;
    }

    @Override
    public void updateSettings(Long workspaceId, WorkspaceSettingsForm form) {
        requireMember(workspaceId);
        Workspace ws = new Workspace();
        ws.setId(workspaceId);
        if (StringUtils.hasText(form.getName())) {
            ws.setName(form.getName());
        }
        if (form.getDescription() != null) {
            ws.setDescription(form.getDescription());
        }
        if (StringUtils.hasText(form.getDefaultModelProvider())) {
            ws.setDefaultModelProvider(form.getDefaultModelProvider());
        }
        if (StringUtils.hasText(form.getLanguage())) {
            ws.setLanguage(form.getLanguage());
        }
        if (form.getSessionRetentionDays() != null) {
            ws.setSessionRetentionDays(form.getSessionRetentionDays());
        }
        if (form.getAutoArchiveDays() != null) {
            ws.setAutoArchiveDays(form.getAutoArchiveDays());
        }
        if (form.getMaxTokensPerTask() != null) {
            ws.setMaxTokensPerTask(form.getMaxTokensPerTask());
        }
        ws.setUpdatedAt(LocalDateTime.now());
        workspaceMapper.updateById(ws);
    }

    @Override
    public List<WorkspaceMemberVO> listMembers(Long workspaceId) {
        requireMember(workspaceId);
        List<WorkspaceMember> members = workspaceMemberMapper.selectList(
                new LambdaQueryWrapper<WorkspaceMember>()
                        .eq(WorkspaceMember::getWorkspaceId, workspaceId)
                        .orderByAsc(WorkspaceMember::getId));
        if (members.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> userIds = members.stream().map(WorkspaceMember::getUserId).collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<WorkspaceMemberVO> list = new ArrayList<>();
        for (WorkspaceMember m : members) {
            User user = userMap.get(m.getUserId());
            if (user == null) {
                continue;
            }
            WorkspaceMemberVO vo = new WorkspaceMemberVO();
            vo.setUserId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
            vo.setAvatar(user.getAvatar());
            vo.setEmail(user.getEmail());
            vo.setRole(m.getRole());
            vo.setRoleLabel(ROLE_LABELS.getOrDefault(m.getRole(), m.getRole()));
            vo.setJoinedAt(m.getJoinedAt());
            vo.setLastActiveAt(m.getLastActiveAt());
            vo.setAgentCount(agentMapper.selectCount(new LambdaQueryWrapper<Agent>()
                    .eq(Agent::getWorkspaceId, workspaceId)
                    .eq(Agent::getCreatedBy, user.getId())));
            vo.setSessionCount30d(sessionMapper.selectCount(new LambdaQueryWrapper<Session>()
                    .eq(Session::getWorkspaceId, workspaceId)
                    .eq(Session::getCreatedBy, user.getId())
                    .ge(Session::getCreatedAt, thirtyDaysAgo)));
            list.add(vo);
        }
        return list;
    }

    @Override
    public void inviteMember(Long workspaceId, MemberInviteForm form) {
        requireMember(workspaceId);
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, form.getEmail()));
        if (user == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该邮箱尚未注册，无法邀请（邀请仅对系统内用户生效）");
        }
        Long exists = workspaceMemberMapper.selectCount(new LambdaQueryWrapper<WorkspaceMember>()
                .eq(WorkspaceMember::getWorkspaceId, workspaceId)
                .eq(WorkspaceMember::getUserId, user.getId()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该用户已是空间成员");
        }
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(workspaceId);
        member.setUserId(user.getId());
        member.setRole(StringUtils.hasText(form.getRole()) ? form.getRole() : "DEVELOPER");
        member.setJoinedAt(LocalDateTime.now());
        member.setLastActiveAt(LocalDateTime.now());
        workspaceMemberMapper.insert(member);
    }

    @Override
    public void updateMemberRole(Long workspaceId, Long userId, String role) {
        requireMember(workspaceId);
        WorkspaceMember member = requireMembership(workspaceId, userId);
        if ("owner".equals(member.getRole())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能修改空间所有者的角色");
        }
        WorkspaceMember upd = new WorkspaceMember();
        upd.setId(member.getId());
        upd.setRole(role);
        workspaceMemberMapper.updateById(upd);
    }

    @Override
    public void removeMember(Long workspaceId, Long userId) {
        requireMember(workspaceId);
        WorkspaceMember member = requireMembership(workspaceId, userId);
        if ("owner".equals(member.getRole())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能移除空间所有者");
        }
        workspaceMemberMapper.deleteById(member.getId());
    }

    @Override
    public PageResult<ActivityLog> pageActivities(Long workspaceId, int page, int pageSize) {
        requireMember(workspaceId);
        Page<ActivityLog> p = new Page<>(page, pageSize);
        Page<ActivityLog> result = activityLogMapper.selectPage(p, new LambdaQueryWrapper<ActivityLog>()
                .eq(ActivityLog::getWorkspaceId, workspaceId)
                .orderByDesc(ActivityLog::getId));
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    // ==================== 辅助 ====================

    /** 空间必须存在且当前用户是其成员 */
    private Workspace requireMember(Long workspaceId) {
        Workspace ws = workspaceMapper.selectById(workspaceId);
        if (ws == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "工作空间不存在");
        }
        Long count = workspaceMemberMapper.selectCount(new LambdaQueryWrapper<WorkspaceMember>()
                .eq(WorkspaceMember::getWorkspaceId, workspaceId)
                .eq(WorkspaceMember::getUserId, SecurityUtils.currentUserId()));
        if (count == null || count == 0) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "工作空间不存在");
        }
        return ws;
    }

    private WorkspaceMember requireMembership(Long workspaceId, Long userId) {
        WorkspaceMember member = workspaceMemberMapper.selectOne(new LambdaQueryWrapper<WorkspaceMember>()
                .eq(WorkspaceMember::getWorkspaceId, workspaceId)
                .eq(WorkspaceMember::getUserId, userId)
                .last("LIMIT 1"));
        if (member == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "该用户不是空间成员");
        }
        return member;
    }
}
