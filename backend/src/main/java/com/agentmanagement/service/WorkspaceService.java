package com.agentmanagement.service;

import com.agentmanagement.common.PageResult;
import com.agentmanagement.entity.ActivityLog;
import com.agentmanagement.form.MemberInviteForm;
import com.agentmanagement.form.WorkspaceCreateForm;
import com.agentmanagement.form.WorkspaceSettingsForm;
import com.agentmanagement.vo.WorkspaceMemberVO;
import com.agentmanagement.vo.WorkspaceSettingsVO;
import com.agentmanagement.vo.WorkspaceVO;

import java.util.List;

/**
 * 工作空间服务：空间列表/创建、设置、成员管理、活动流。
 */
public interface WorkspaceService {

    /** 当前用户的空间列表（含我在其中的角色与统计） */
    List<WorkspaceVO> myWorkspaces();

    /** 创建空间：写入 workspace 并把创建者加为 owner */
    WorkspaceVO create(WorkspaceCreateForm form);

    /** 读取空间设置 */
    WorkspaceSettingsVO getSettings(Long workspaceId);

    /** 保存空间设置（仅非 null 字段生效） */
    void updateSettings(Long workspaceId, WorkspaceSettingsForm form);

    /** 成员列表（带 Agent 数 / 30 天会话数统计） */
    List<WorkspaceMemberVO> listMembers(Long workspaceId);

    /** 按邮箱邀请（系统内已注册用户直接加入空间；未注册报错） */
    void inviteMember(Long workspaceId, MemberInviteForm form);

    /** 修改成员角色（owner 不可改） */
    void updateMemberRole(Long workspaceId, Long userId, String role);

    /** 移除成员（owner 不可移除） */
    void removeMember(Long workspaceId, Long userId);

    /** 空间活动流（分页） */
    PageResult<ActivityLog> pageActivities(Long workspaceId, int page, int pageSize);
}
