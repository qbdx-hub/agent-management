package com.agentmanagement.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 创建自定义角色请求体。
 */
@Data
public class RoleCreateForm {

    /** 角色标识（如 reviewer），同空间内唯一 */
    @NotBlank(message = "角色标识不能为空")
    private String name;

    @NotBlank(message = "角色显示名不能为空")
    private String label;

    private String description;

    /** 权限列表，如 ["agent:read", "tool:register"] */
    private List<String> permissions;
}
