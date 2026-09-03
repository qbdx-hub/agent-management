package com.agentmanagement.controller;

import com.agentmanagement.common.PageResult;
import com.agentmanagement.common.Result;
import com.agentmanagement.common.annotation.AuditLog;
import com.agentmanagement.entity.ActivityLog;
import com.agentmanagement.form.MemberInviteForm;
import com.agentmanagement.form.MemberRoleForm;
import com.agentmanagement.form.WorkspaceCreateForm;
import com.agentmanagement.form.WorkspaceSettingsForm;
import com.agentmanagement.service.WorkspaceService;
import com.agentmanagement.vo.WorkspaceMemberVO;
import com.agentmanagement.vo.WorkspaceSettingsVO;
import com.agentmanagement.vo.WorkspaceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 工作空间 RESTful 接口（前缀 /api/v1 由 context-path 统一加）。
 * GET /workspaces 我的空间列表、POST /workspaces 创建；
 * GET|PUT /workspaces/{id}/settings 空间设置；
 * GET|POST /workspaces/{id}/members 成员列表/邀请、PUT|DELETE /workspaces/{id}/members/{userId} 角色与移除；
 * GET /workspaces/{id}/activities 空间活动流。
 */
@RestController
@RequestMapping("/workspaces")
public class WorkspaceController {

    @Autowired
    private WorkspaceService workspaceService;

    /** GET /workspaces —— 当前用户的空间列表（顶栏切换器） */
    @GetMapping
    public Result<List<WorkspaceVO>> my() {
        return Result.success(workspaceService.myWorkspaces());
    }

    /** POST /workspaces —— 创建空间（创建者自动成为 owner） */
    @AuditLog(action = "workspace.create", label = "创建工作空间", resourceType = "workspace")
    @PostMapping
    public Result<WorkspaceVO> create(@Valid @RequestBody WorkspaceCreateForm form) {
        return Result.success(workspaceService.create(form));
    }

    /** GET /workspaces/{id}/settings —— 空间设置 */
    @GetMapping("/{id}/settings")
    public Result<WorkspaceSettingsVO> settings(@PathVariable("id") Long id) {
        return Result.success(workspaceService.getSettings(id));
    }

    /** PUT /workspaces/{id}/settings —— 保存设置（含名称/描述） */
    @AuditLog(action = "workspace.update", label = "更新空间设置", resourceType = "workspace")
    @PutMapping("/{id}/settings")
    public Result<Void> updateSettings(@PathVariable("id") Long id,
                                       @RequestBody WorkspaceSettingsForm form) {
        workspaceService.updateSettings(id, form);
        return Result.success();
    }

    /** GET /workspaces/{id}/members —— 成员列表 */
    @GetMapping("/{id}/members")
    public Result<List<WorkspaceMemberVO>> members(@PathVariable("id") Long id) {
        return Result.success(workspaceService.listMembers(id));
    }

    /** POST /workspaces/{id}/members —— 邀请成员（系统内用户直接加入） */
    @AuditLog(action = "member.invite", label = "邀请成员", resourceType = "workspace_member")
    @PostMapping("/{id}/members")
    public Result<Void> invite(@PathVariable("id") Long id,
                               @Valid @RequestBody MemberInviteForm form) {
        workspaceService.inviteMember(id, form);
        return Result.success();
    }

    /** PUT /workspaces/{id}/members/{userId} —— 修改成员角色 */
    @AuditLog(action = "member.role_update", label = "修改成员角色", resourceType = "workspace_member")
    @PutMapping("/{id}/members/{userId}")
    public Result<Void> updateRole(@PathVariable("id") Long id,
                                   @PathVariable("userId") Long userId,
                                   @Valid @RequestBody MemberRoleForm form) {
        workspaceService.updateMemberRole(id, userId, form.getRole());
        return Result.success();
    }

    /** DELETE /workspaces/{id}/members/{userId} —— 移除成员 */
    @AuditLog(action = "member.remove", label = "移除成员", resourceType = "workspace_member")
    @DeleteMapping("/{id}/members/{userId}")
    public Result<Void> removeMember(@PathVariable("id") Long id,
                                     @PathVariable("userId") Long userId) {
        workspaceService.removeMember(id, userId);
        return Result.success();
    }

    /** GET /workspaces/{id}/activities —— 空间活动流（分页） */
    @GetMapping("/{id}/activities")
    public Result<PageResult<ActivityLog>> activities(@PathVariable("id") Long id,
                                                      @RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(workspaceService.pageActivities(id, page, pageSize));
    }
}
