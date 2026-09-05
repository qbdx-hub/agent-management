package com.agentmanagement.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * POST /terminal/exec —— 命令执行结果。
 *
 * <p>timedOut 时不视为业务失败（code 仍为 0），前端按系统行渲染「已超时终止」。
 */
@Data
public class TerminalExecVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 合并后的 stdout+stderr 输出（已 trim，超长截断） */
    private String output;

    /** 退出码；超时时为 -1 */
    private Integer exitCode;

    /** 执行耗时毫秒（cd 特判等不 spawn 进程的命令为 0） */
    private Long durationMs;

    /** 执行后的工作目录（相对沙箱根，'' 即根）；cd 成功时为新目录 */
    private String cwd;

    /** 输出是否因超长被截断 */
    private Boolean truncated;

    /** 是否超时被强制终止 */
    private Boolean timedOut;
}
