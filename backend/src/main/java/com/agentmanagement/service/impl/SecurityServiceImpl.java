package com.agentmanagement.service.impl;

import com.agentmanagement.common.BusinessException;
import com.agentmanagement.common.PageResult;
import com.agentmanagement.common.ResultCode;
import com.agentmanagement.entity.Approval;
import com.agentmanagement.entity.ApprovalRule;
import com.agentmanagement.entity.Role;
import com.agentmanagement.entity.UserRole;
import com.agentmanagement.mapper.ApprovalMapper;
import com.agentmanagement.mapper.ApprovalRuleMapper;
import com.agentmanagement.mapper.RoleMapper;
import com.agentmanagement.mapper.UserRoleMapper;
import com.agentmanagement.security.SecurityUtils;
import com.agentmanagement.form.RoleCreateForm;
import com.agentmanagement.service.SecurityService;
import com.agentmanagement.service.WorkflowExecutionService;
import com.agentmanagement.vo.ApprovalRuleVO;
import com.agentmanagement.vo.ApprovalVO;
import com.agentmanagement.vo.RoleVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 安全中心服务实现。
 * 角色模型：role.workspace_id 为空 = 系统角色（全空间只读共享），否则为本空间自定义角色；
 * 成员数实时统计自 user_role。
 * 审批模型：approval 单由工作流审批节点等业务方创建；处理 workflow_run 类型时
 * 联动 WorkflowExecutionService 恢复/终止对应运行，其余类型直接回填审批单。
 */
@Service
public class SecurityServiceImpl implements SecurityService {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private ApprovalMapper approvalMapper;

    @Autowired
    private ApprovalRuleMapper approvalRuleMapper;

    @Autowired
    private WorkflowExecutionService workflowExecutionService;

    @Override
    public List<RoleVO> listRoles() {
        Long workspaceId = SecurityUtils.currentWorkspaceId();
        List<Role> roles = roleMapper.selectList(new LambdaQueryWrapper<Role>()
                .and(w -> w.eq(Role::getWorkspaceId, workspaceId).or().isNull(Role::getWorkspaceId))
                .orderByAsc(Role::getId));
        List<RoleVO> list = new ArrayList<>();
        for (Role role : roles) {
            RoleVO vo = new RoleVO();
            vo.setId(role.getId());
            vo.setName(role.getName());
            vo.setLabel(role.getLabel());
            vo.setDescription(role.getDescription());
            vo.setIsSystem(role.getIsSystem() != null && role.getIsSystem() == 1);
            vo.setMemberCount(userRoleMapper.selectCount(
                    new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, role.getId())));
            vo.setPermissions(role.getPermissions());
            list.add(vo);
        }
        return list;
    }

    @Override
    public RoleVO createRole(RoleCreateForm form) {
        Long workspaceId = SecurityUtils.currentWorkspaceId();
        Long exists = roleMapper.selectCount(new LambdaQueryWrapper<Role>()
                .eq(Role::getWorkspaceId, workspaceId)
                .eq(Role::getName, form.getName()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "角色标识「" + form.getName() + "」已存在");
        }
        Role role = new Role();
        role.setWorkspaceId(workspaceId);
        role.setName(form.getName());
        role.setLabel(form.getLabel());
        role.setDescription(form.getDescription());
        role.setPermissions(form.getPermissions());
        role.setIsSystem(0);
        role.setCreatedAt(LocalDateTime.now());
        roleMapper.insert(role);

        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setName(role.getName());
        vo.setLabel(role.getLabel());
        vo.setDescription(role.getDescription());
        vo.setIsSystem(false);
        vo.setMemberCount(0L);
        vo.setPermissions(role.getPermissions());
        return vo;
    }

    @Override
    public void deleteRole(Long roleId) {
        Role role = roleMapper.selectById(roleId);
        if (role == null || (role.getWorkspaceId() != null
                && !SecurityUtils.currentWorkspaceId().equals(role.getWorkspaceId()))) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "角色不存在");
        }
        if (role.getIsSystem() != null && role.getIsSystem() == 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "系统角色不可删除");
        }
        roleMapper.deleteById(roleId);
        // 同步清理用户关联
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId));
    }

    @Override
    public List<ApprovalRuleVO> listApprovalRules() {
        List<ApprovalRule> rules = approvalRuleMapper.selectList(new LambdaQueryWrapper<ApprovalRule>()
                .eq(ApprovalRule::getWorkspaceId, SecurityUtils.currentWorkspaceId())
                .orderByAsc(ApprovalRule::getId));
        return rules.stream().map(this::toRuleVO).collect(Collectors.toList());
    }

    @Override
    public PageResult<ApprovalVO> pageApprovals(String status, int page, int pageSize) {
        Page<Approval> p = new Page<>(page, pageSize);
        Page<Approval> result = approvalMapper.selectPage(p, new LambdaQueryWrapper<Approval>()
                .eq(Approval::getWorkspaceId, SecurityUtils.currentWorkspaceId())
                .eq(StringUtils.hasText(status), Approval::getStatus, status)
                .orderByDesc(Approval::getId));
        List<ApprovalVO> list = result.getRecords().stream().map(this::toApprovalVO).collect(Collectors.toList());
        return PageResult.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public void resolveApproval(Long approvalId, boolean approved, String comment) {
        Approval approval = approvalMapper.selectById(approvalId);
        if (approval == null || !SecurityUtils.currentWorkspaceId().equals(approval.getWorkspaceId())) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "审批记录不存在");
        }
        if (!"pending".equals(approval.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该审批已处理");
        }

        // 工作流审批：联动恢复/终止对应运行（approveRun 内部回填审批单）
        if ("workflow_run".equals(approval.getResourceType()) && approval.getResourceId() != null) {
            try {
                workflowExecutionService.approveRun(approval.getResourceId(), approved, comment,
                        SecurityUtils.currentUserId());
                return;
            } catch (BusinessException e) {
                // 运行已不在等待状态（可能已被处理），退回直接回填审批单
            }
        }

        Approval upd = new Approval();
        upd.setId(approval.getId());
        upd.setStatus(approved ? "approved" : "rejected");
        upd.setApproverId(SecurityUtils.currentUserId());
        upd.setReason(comment);
        upd.setResolvedAt(LocalDateTime.now());
        approvalMapper.updateById(upd);
    }

    // ==================== 辅助 ====================

    private ApprovalRuleVO toRuleVO(ApprovalRule rule) {
        ApprovalRuleVO vo = new ApprovalRuleVO();
        vo.setId(rule.getId());
        vo.setName(rule.getName());
        vo.setTriggerAction(rule.getTriggerAction());
        vo.setTriggerCondition(rule.getTriggerCondition() != null ? rule.getTriggerCondition().toString() : "");
        vo.setApproverRole(rule.getApproverRole());
        vo.setRequiredApprovals(rule.getRequiredApprovals());
        vo.setEnabled(rule.getEnabled() != null && rule.getEnabled() == 1);
        return vo;
    }

    private ApprovalVO toApprovalVO(Approval approval) {
        ApprovalVO vo = new ApprovalVO();
        vo.setApprovalId(approval.getId());
        vo.setRuleId(approval.getRuleId());
        vo.setRuleName(approval.getRuleName());
        vo.setResourceType(approval.getResourceType());
        vo.setResourceId(approval.getResourceId());
        vo.setResourceName(approval.getResourceName());
        vo.setAction(approval.getAction());
        vo.setDetail(approval.getDetail());
        vo.setApplicantId(approval.getApplicantId());
        vo.setApplicantName(approval.getApplicantName());
        vo.setApproverName(approval.getApproverName());
        vo.setStatus(approval.getStatus());
        vo.setReason(approval.getReason());
        vo.setCreatedAt(approval.getCreatedAt());
        vo.setResolvedAt(approval.getResolvedAt());
        return vo;
    }
}
