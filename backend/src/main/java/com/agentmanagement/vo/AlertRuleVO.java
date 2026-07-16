package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 告警规则 VO —— 对齐前端 AlertRule 类型。
 * 将 DB Integer(0/1) 的 enabled 转为前端 Boolean。
 */
@Data
public class AlertRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
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
