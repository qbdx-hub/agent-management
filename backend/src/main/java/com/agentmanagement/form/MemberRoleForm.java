package com.agentmanagement.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 修改成员角色请求体。
 */
@Data
public class MemberRoleForm {

    @NotBlank(message = "角色不能为空")
    private String role;
}
