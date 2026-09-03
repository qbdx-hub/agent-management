package com.agentmanagement.service;

import com.agentmanagement.common.PageResult;
import com.agentmanagement.form.RoleCreateForm;
import com.agentmanagement.vo.ApprovalVO;
import com.agentmanagement.vo.ApprovalRuleVO;
import com.agentmanagement.vo.RoleVO;

import java.util.List;

/**
 * 安全中心服务：角色管理 + 审批中心。
 */
public interface SecurityService {

    /** 角色列表（本空间自定义角色 + 系统角色），含成员数统计 */
    List<RoleVO> listRoles();

    /** 创建自定义角色 */
    RoleVO createRole(RoleCreateForm form);

    /** 删除自定义角色（系统角色不可删） */
    void deleteRole(Long roleId);

    /** 审批规则列表（本空间） */
    List<ApprovalRuleVO> listApprovalRules();

    /** 审批记录分页（可按状态过滤） */
    PageResult<ApprovalVO> pageApprovals(String status, int page, int pageSize);

    /**
     * 处理审批：workflow_run 类型会联动恢复/终止工作流运行；
     * 其余类型直接回填审批单。
     */
    void resolveApproval(Long approvalId, boolean approved, String comment);
}
