package com.agentmanagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 告警记录表实体（对应 alert_record 表）。
 * 告警规则触发后生成的记录，含当前值、阈值与状态。
 */
@Data
@TableName("alert_record")
public class AlertRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long ruleId;

    /** 规则名称（冗余） */
    private String ruleName;

    private Long workspaceId;

    private Long agentId;

    /** 级别：info/warning/critical */
    private String severity;

    /** 告警信息 */
    private String message;

    /** 当前值 */
    private String currentValue;

    /** 阈值 */
    private String thresholdValue;

    /** 状态：triggered/resolved/ignored */
    private String status;

    /** 触发时间 */
    private LocalDateTime triggeredAt;

    /** 解决时间 */
    private LocalDateTime resolvedAt;
}
