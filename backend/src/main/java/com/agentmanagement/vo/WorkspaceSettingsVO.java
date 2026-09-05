package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 空间设置 VO（基本信息 + Agent 执行环境策略）。
 */
@Data
public class WorkspaceSettingsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;

    private String description;

    /** 共享工作目录：0-每会话独立沙箱，1-空间内会话共享文件区 */
    private Boolean sharedWorkdir;

    /** 允许沙箱外运行总闸 */
    private Boolean allowOutsideSandbox;

    /** 空间级禁用的内置工具名 */
    private List<String> disabledTools;

    /** 成员终端开关：空间成员是否可用移动端终端 */
    private Boolean memberTerminalEnabled;
}
