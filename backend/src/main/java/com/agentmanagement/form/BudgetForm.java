package com.agentmanagement.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 创建/修改预算表单。
 * 前端字段对齐：scope/period 用字符串，meltdownEnabled/enabled 用 Boolean（前端发 true/false），
 * 后端 Budget 实体用 Integer(0/1)，由 Service 层做转换。
 */
@Data
public class BudgetForm {

    @NotBlank(message = "名称不能为空")
    private String name;

    /** 范围：workspace/user/agent */
    @NotBlank(message = "范围不能为空")
    private String scope;

    /** 范围 ID（用户 ID 或 Agent ID），workspace 范围时可为 null */
    private Long scopeId;

    /** 周期：daily/monthly */
    @NotBlank(message = "周期不能为空")
    private String period;

    /** 预算限额（美元） */
    @NotNull(message = "限额不能为空")
    private BigDecimal limitAmount;

    /** 告警阈值百分比 */
    @NotNull(message = "告警阈值不能为空")
    private Integer warnPercent;

    /** 超支熔断是否启用（前端 Boolean，Service 转 Integer 0/1） */
    @NotNull(message = "熔断设置不能为空")
    private Boolean meltdownEnabled;

    /** 通知渠道列表 */
    private List<String> notifyChannels;

    /** 是否启用（前端 Boolean，Service 转 Integer 0/1） */
    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;
}
