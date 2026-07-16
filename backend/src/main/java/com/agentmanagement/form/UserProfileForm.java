package com.agentmanagement.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 修改个人信息请求表单（对应 PUT /api/v1/auth/profile）。
 * 用户名、昵称可修改；密码修改需先校验旧密码。
 */
@Data
public class UserProfileForm {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,50}$", message = "用户名只能包含字母、数字、下划线、短横线，长度3-50")
    private String username;

    @NotBlank(message = "昵称不能为空")
    @Pattern(regexp = "^[^\\u0000-\\u001F]{1,50}$", message = "昵称长度1-50且不能含控制字符")
    private String nickname;

    @Size(max = 100, message = "邮箱长度不能超过100")
    private String email;

    /** 旧密码：仅在设置新密码时必填 */
    private String oldPassword;

    /** 新密码：为空表示不修改密码 */
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z0-9!@#$%^&*._-]{8,50}$", message = "密码需8-50位且包含字母和数字")
    private String newPassword;
}
