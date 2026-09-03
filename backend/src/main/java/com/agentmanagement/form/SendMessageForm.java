package com.agentmanagement.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 发送消息表单（对应前端 SendMessageDTO）。
 */
@Data
public class SendMessageForm {

    @NotBlank(message = "消息内容不能为空")
    private String content;

    /** 执行模式：auto/step_by_step/plan_only */
    private String mode;

    /** 附件路径列表 */
    private List<String> attachments;

    /** 用户授权：本次消息允许在沙箱外运行（真实文件系统/工作目录，默认 false 限会话沙箱内） */
    private Boolean outsideSandbox;
}
