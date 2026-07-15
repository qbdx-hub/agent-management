package com.agentmanagement.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Map;

/**
 * 创建知识库请求表单。
 */
@Data
public class KnowledgeBaseCreateForm {

    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 100, message = "知识库名称不能超过100个字符")
    private String name;

    @Size(max = 500, message = "描述不能超过500个字符")
    private String description;

    /** 类型：vector/keyword/hybrid，默认 vector */
    private String type;

    /** 嵌入模型 */
    private String embeddingModel;

    /** 配置 JSON（chunk_size/overlap 等） */
    private Map<String, Object> config;
}
