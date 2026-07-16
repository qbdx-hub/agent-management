package com.agentmanagement.form;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 告警规则创建/编辑表单 —— 对齐前端 createAlertRule 参数。
 * 前端传 Boolean enabled / Double threshold，DB 存 Integer(0/1) / BigDecimal。
 */
@Data
public class AlertRuleForm implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String metric;
    private String targetType;
    private Long targetId;
    private String condition;
    private Double threshold;
    private String duration;
    private String severity;
    private Boolean enabled;
    private List<String> notifyChannels;
}
