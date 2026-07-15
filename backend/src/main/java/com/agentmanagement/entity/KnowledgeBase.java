package com.agentmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 知识库表实体（对应 knowledge_base 表）。
 * 知识库元信息与检索/分块配置，documentCount/totalTokens 为冗余统计。
 * config 为 JSON 列（chunk_size/overlap 等），用 JacksonTypeHandler；需 autoResultMap = true。
 */
@Data
@TableName(value = "knowledge_base", autoResultMap = true)
public class KnowledgeBase implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long workspaceId;

    private String name;

    private String description;

    /** 类型：vector/keyword/hybrid */
    private String type;

    /** 嵌入模型 */
    private String embeddingModel;

    private Integer documentCount;

    private Long totalTokens;

    /** 状态：active/building/error */
    private String status;

    /** 配置 JSON（chunk_size/overlap 等） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> config;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
