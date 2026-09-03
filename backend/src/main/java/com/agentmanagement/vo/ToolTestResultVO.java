package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 工具连通性测试结果 VO（对应前端 ToolTestResult）。
 */
@Data
public class ToolTestResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean success;

    private Integer latencyMs;

    private String requestUrl;

    private String requestBody;

    private Integer responseStatus;

    /** 响应体（截断至 64KB，保护前端渲染与传输） */
    private String responseBody;

    /** 按 responseMapping 映射后的输出（未配置时为原始响应体） */
    private String mappedOutput;
}
