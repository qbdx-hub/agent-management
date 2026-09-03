package com.agentmanagement.form;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

/**
 * 邀请成员请求体。
 */
@Data
public class MemberInviteForm {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 空间内角色：ADMIN/MANAGER/DEVELOPER/VIEWER，默认 DEVELOPER */
    private String role;
}
