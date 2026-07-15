package com.agentmanagement.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * 注册/新建工具表单（对应前端 ToolRegisterDTO）。
 * endpoint 为前端嵌套结构 {url,method,headers,timeoutMs}，后端落库时扁平化到 tool 表。
 */
@Data
public class ToolRegisterForm {

    @NotBlank(message = "工具标识名不能为空")
    @Size(max = 100, message = "工具标识名不能超过100字")
    private String name;

    @NotBlank(message = "工具显示名不能为空")
    @Size(max = 100, message = "工具显示名不能超过100字")
    private String displayName;

    @Size(max = 500, message = "描述不能超过500字")
    private String description;

    /** 分类：search/compute/operate/perceive/notify/custom */
    private String category;

    private String icon;

    /** 类型：api/mcp/builtin；不传时后端默认 api */
    private String type;

    /** 端点配置（api 类型工具） */
    private Endpoint endpoint;

    /** 参数定义数组 */
    private List<Map<String, Object>> parameters;

    /** 响应映射表达式 */
    private String responseMapping;

    /** 凭证引用标识 */
    private String credentialRef;

    private Boolean retryOnFail;

    private Integer maxRetries;

    /** 端点嵌套结构（对应前端 ToolEndpoint） */
    @Data
    public static class Endpoint {
        private String url;
        /** GET/POST/PUT/DELETE/PATCH */
        private String method;
        private Map<String, String> headers;
        private Integer timeoutMs;
    }
}
