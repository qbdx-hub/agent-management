package com.agentmanagement.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * POST /security/api-keys —— 新建 API 密钥请求体。
 */
@Data
public class ApiKeyCreateForm {

    @NotBlank(message = "密钥名称不能为空")
    @Size(max = 64, message = "密钥名称最长64字符")
    private String name;
}
