package com.agentmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档分块表实体（对应 document_chunk 表）。
 * 存储文档切分后的文本块及其向量 embedding。
 * embedding 为 JSON 列，用 JacksonTypeHandler。
 */
@Data
@TableName(value = "document_chunk", autoResultMap = true)
public class DocumentChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private Long knowledgeBaseId;

    /** 块序号（从 0 开始） */
    private Integer chunkIndex;

    /** 文本内容 */
    private String content;

    /** Token 数（估算） */
    private Integer tokenCount;

    /** 向量 JSON 数组 [float, float, ...] */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Float> embedding;

    private LocalDateTime createdAt;
}
