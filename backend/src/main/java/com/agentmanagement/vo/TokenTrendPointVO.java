package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * Token 用量趋势数据点 VO —— 对齐前端 TokenTrendPoint 类型。
 */
@Data
public class TokenTrendPointVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String time;
    private Long input;
    private Long output;
}
