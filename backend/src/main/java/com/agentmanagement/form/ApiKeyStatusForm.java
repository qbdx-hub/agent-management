package com.agentmanagement.form;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * PUT /security/api-keys/{id}/status —— 启用/停用请求体。
 */
@Data
public class ApiKeyStatusForm {

    @NotNull(message = "enabled 不能为空")
    private Boolean enabled;
}
