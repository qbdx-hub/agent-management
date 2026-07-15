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
 * 文档表实体（对应 document 表）。
 * 知识库内的单个文档，含文件信息、分块/Token 统计与处理状态。
 * metadata 为 JSON 列，用 JacksonTypeHandler；需 autoResultMap = true。
 */
@Data
@TableName(value = "document", autoResultMap = true)
public class Document implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long knowledgeBaseId;

    /** 文件名 */
    private String name;

    /** 文件类型：pdf/txt/md/docx/csv */
    private String fileType;

    /** 文件大小（bytes） */
    private Long fileSize;

    private String fileUrl;

    /** 分块数 */
    private Integer chunkCount;

    private Long totalTokens;

    /** 状态：pending/processing/completed/failed */
    private String status;

    /** 错误信息 */
    private String error;

    /** 元数据 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;

    /** 上传者 ID */
    private Long uploadedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
