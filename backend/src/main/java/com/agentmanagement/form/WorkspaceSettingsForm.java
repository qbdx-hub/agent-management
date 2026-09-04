package com.agentmanagement.form;

import lombok.Data;

import java.util.List;

/**
 * 空间设置保存请求体。
 * name/description 与执行策略一起提交；null 字段表示不修改。
 */
@Data
public class WorkspaceSettingsForm {

    /** 空间名称（null 表示不修改） */
    private String name;

    private String description;

    /** 共享工作目录（null 表示不修改） */
    private Boolean sharedWorkdir;

    /** 允许沙箱外运行总闸（null 表示不修改） */
    private Boolean allowOutsideSandbox;

    /** 空间级禁用的内置工具名（null 表示不修改；空列表=全部允许） */
    private List<String> disabledTools;
}
