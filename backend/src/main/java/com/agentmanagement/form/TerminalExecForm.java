package com.agentmanagement.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * POST /terminal/exec —— 终端执行命令请求体。
 *
 * <p>cwd 是相对沙箱根的路径（客户端持有，服务端无状态），空串/缺省=沙箱根。
 */
@Data
public class TerminalExecForm {

    @NotBlank(message = "命令不能为空")
    @Size(max = 500, message = "命令最长500字符")
    private String command;

    /** 当前工作目录（相对沙箱根，'/' 分隔），空=根目录 */
    @Size(max = 500, message = "cwd 最长500字符")
    private String cwd;

    /**
     * 审计切面通过 getName() 反射取 resourceName——把执行的命令记入审计日志。
     * audit_log.resource_name 是 VARCHAR(100) 且服务层无截断，超长会导致审计写入失败，
     * 因此这里必须自行截断到 100 字符。
     */
    public String getName() {
        String cmd = command == null ? "" : command.trim();
        return cmd.length() <= 100 ? cmd : cmd.substring(0, 100);
    }
}
