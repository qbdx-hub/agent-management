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
 * 角色表实体（对应 role 表）。
 * permissions 是 JSON 列，用 JacksonTypeHandler 自动序列化为 List<String>。
 * 必须加 autoResultMap = true，否则 typeHandler 不触发、permissions 永远为 null。
 */
@Data
@TableName(value = "role", autoResultMap = true)
public class Role implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 工作空间ID（NULL = 系统角色） */
    private Long workspaceId;

    /** 角色标识：super_admin / admin / developer / observer */
    private String name;

    private String label;

    private String description;

    /** 权限列表 JSON 数组，如 ["agent:*","tool:read"] */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> permissions;

    /** 是否系统角色：0-否 1-是 */
    private Integer isSystem;

    private LocalDateTime createdAt;
}
