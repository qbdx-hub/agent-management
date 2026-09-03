package com.agentmanagement.form;

import lombok.Data;

/**
 * 空间设置保存请求体。
 * name/description 与运行配置一起提交；null 字段表示不修改。
 */
@Data
public class WorkspaceSettingsForm {

    /** 空间名称（null 表示不修改） */
    private String name;

    private String description;

    private String defaultModelProvider;

    private String language;

    private Integer sessionRetentionDays;

    private Integer autoArchiveDays;

    private Long maxTokensPerTask;
}
