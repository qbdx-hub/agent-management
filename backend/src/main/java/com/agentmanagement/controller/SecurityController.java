package com.agentmanagement.controller;

import com.agentmanagement.common.PageResult;
import com.agentmanagement.common.Result;
import com.agentmanagement.common.annotation.AuditLog;
import com.agentmanagement.form.ApiKeyCreateForm;
import com.agentmanagement.form.ApiKeyStatusForm;
import com.agentmanagement.form.ApprovalDecisionForm;
import com.agentmanagement.form.RoleCreateForm;
import com.agentmanagement.service.ApiKeyService;
import com.agentmanagement.service.SecurityService;
import com.agentmanagement.vo.ApiKeyVO;
import com.agentmanagement.vo.ApprovalRuleVO;
import com.agentmanagement.vo.ApprovalVO;
import com.agentmanagement.vo.RoleVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 安全中心 RESTful 接口（前缀 /api/v1 由 context-path 统一加）。
 * 角色：GET|POST /security/roles、DELETE /security/roles/{id}；
 * 审批：GET /security/approvals/rules、GET /security/approvals 分页（status 过滤）、
 * POST /security/approvals/{id}/approve、POST /security/approvals/{id}/reject。
 */
@RestController
@RequestMapping("/security")
public class SecurityController {

    @Autowired
    private SecurityService securityService;

    @Autowired
    private ApiKeyService apiKeyService;

    /** GET /security/roles —— 角色列表（自定义 + 系统） */
    @GetMapping("/roles")
    public Result<List<RoleVO>> roles() {
        return Result.success(securityService.listRoles());
    }

    /** POST /security/roles —— 创建自定义角色 */
    @AuditLog(action = "role.create", label = "创建角色", resourceType = "role")
    @PostMapping("/roles")
    public Result<RoleVO> createRole(@Valid @RequestBody RoleCreateForm form) {
        return Result.success(securityService.createRole(form));
    }

    /** DELETE /security/roles/{id} —— 删除自定义角色 */
    @AuditLog(action = "role.delete", label = "删除角色", resourceType = "role")
    @DeleteMapping("/roles/{id}")
    public Result<Void> deleteRole(@PathVariable("id") Long id) {
        securityService.deleteRole(id);
        return Result.success();
    }

    /** GET /security/approvals/rules —— 审批规则列表 */
    @GetMapping("/approvals/rules")
    public Result<List<ApprovalRuleVO>> approvalRules() {
        return Result.success(securityService.listApprovalRules());
    }

    /** GET /security/approvals —— 审批记录分页（status=pending/approved/rejected，可省略） */
    @GetMapping("/approvals")
    public Result<PageResult<ApprovalVO>> approvals(@RequestParam(required = false) String status,
                                                    @RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(securityService.pageApprovals(status, page, pageSize));
    }

    /** POST /security/approvals/{id}/approve —— 通过（workflow_run 类型联动恢复运行） */
    @AuditLog(action = "approval.approve", label = "通过审批", resourceType = "approval")
    @PostMapping("/approvals/{id}/approve")
    public Result<Void> approve(@PathVariable("id") Long id,
                                @RequestBody(required = false) ApprovalDecisionForm form) {
        securityService.resolveApproval(id, true, form != null ? form.getComment() : null);
        return Result.success();
    }

    /** POST /security/approvals/{id}/reject —— 拒绝（workflow_run 类型联动终止运行） */
    @AuditLog(action = "approval.reject", label = "拒绝审批", resourceType = "approval")
    @PostMapping("/approvals/{id}/reject")
    public Result<Void> reject(@PathVariable("id") Long id,
                               @RequestBody(required = false) ApprovalDecisionForm form) {
        securityService.resolveApproval(id, false, form != null ? form.getComment() : null);
        return Result.success();
    }

    // ==================== API 密钥（移动端「API 密钥管理」屏） ====================

    /** GET /security/api-keys —— 当前用户的密钥列表（只含掩码，明文永不返回） */
    @GetMapping("/api-keys")
    public Result<List<ApiKeyVO>> apiKeys() {
        return Result.success(apiKeyService.listByCurrentUser());
    }

    /** POST /security/api-keys —— 新建（完整明文仅本次响应返回一次），每用户上限 5 个 */
    @AuditLog(action = "api_key.create", label = "创建API密钥", resourceType = "api_key")
    @PostMapping("/api-keys")
    public Result<ApiKeyVO> createApiKey(@Valid @RequestBody ApiKeyCreateForm form) {
        return Result.success(apiKeyService.create(form));
    }

    /** PUT /security/api-keys/{id}/status —— 启用/停用 */
    @PutMapping("/api-keys/{id}/status")
    public Result<Void> updateApiKeyStatus(@PathVariable("id") Long id,
                                           @Valid @RequestBody ApiKeyStatusForm form) {
        apiKeyService.updateStatus(id, Boolean.TRUE.equals(form.getEnabled()));
        return Result.success();
    }

    /** DELETE /security/api-keys/{id} —— 删除（泄露场景 = 删除后新建） */
    @AuditLog(action = "api_key.delete", label = "删除API密钥", resourceType = "api_key")
    @DeleteMapping("/api-keys/{id}")
    public Result<Void> deleteApiKey(@PathVariable("id") Long id) {
        apiKeyService.delete(id);
        return Result.success();
    }
}
