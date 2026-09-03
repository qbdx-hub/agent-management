package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间设置 VO（对应 workspace 表的运行配置列）。
 */
@Data
public class WorkspaceSettingsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String defaultModelProvider;

    private Integer sessionRetentionDays;

    private Integer autoArchiveDays;

    private Long maxTokensPerTask;

    private String language;
}
