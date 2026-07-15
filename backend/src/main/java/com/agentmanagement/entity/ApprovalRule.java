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
 * 审批规则表实体（对应 approval_rule 表）。
 * 定义某资源/操作需经指定角色审批的规则。
 * triggerCondition 为 JSON 列，用 JacksonTypeHandler；需 autoResultMap = true。
 */
@Data
@TableName(value = "approval_rule", autoResultMap = true)
public class ApprovalRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long workspaceId;

    private String name;

    private String description;

    /** 资源类型：agent/tool */
    private String resourceType;

    /** 触发操作：publish/register/delete */
    private String triggerAction;

    /** 触发条件 JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> triggerCondition;

    /** 审批角色 */
    private String approverRole;

    /** 所需审批人数 */
    private Integer requiredApprovals;

    /** 是否启用：0-否 1-是 */
    private Integer enabled;

    private LocalDateTime createdAt;
}
