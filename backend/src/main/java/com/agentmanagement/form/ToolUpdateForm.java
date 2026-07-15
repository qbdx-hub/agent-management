package com.agentmanagement.form;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 编辑工具表单（对应前端 Partial&lt;ToolRegisterDTO&gt;，字段全部可选）。
 * 复用 ToolRegisterForm.Endpoint 作为端点嵌套结构。
 */
@Data
public class ToolUpdateForm {

    private String name;

    private String displayName;

    private String description;

    private String category;

    private String icon;

    private String type;

    private ToolRegisterForm.Endpoint endpoint;

    private List<Map<String, Object>> parameters;

    private String responseMapping;

    private String credentialRef;

    private Boolean retryOnFail;

    private Integer maxRetries;
}
