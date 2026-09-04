package com.agentmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作空间表实体（对应 workspace 表）。
 */
@Data
@TableName("workspace")
public class Workspace implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String icon;

    private Long ownerId;

    /** 状态：0-禁用 1-启用 */
    private Integer status;

    /** 共享工作目录：0-每会话独立沙箱，1-空间内会话共享文件区 */
    private Boolean sharedWorkdir;

    /** 允许沙箱外运行总闸：0-禁止成员授权逃逸沙箱，1-允许 */
    private Boolean allowOutsideSandbox;

    /** 空间级禁用的内置工具名，逗号分隔；NULL/空=全部允许 */
    private String disabledTools;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
