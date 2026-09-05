package com.agentmanagement.service;

import com.agentmanagement.form.TerminalExecForm;
import com.agentmanagement.vo.TerminalExecVO;
import com.agentmanagement.vo.TerminalInfoVO;

/**
 * 移动端命令终端：在当前工作空间的沙箱目录内执行 shell 命令。
 * 仅空间 owner/admin 可用，所有执行全量审计。
 */
public interface TerminalService {

    /** 终端环境信息（os/角色/沙箱目录名） */
    TerminalInfoVO info();

    /** 执行命令；cwd 由客户端持有并在每次请求中回传，服务端无状态 */
    TerminalExecVO exec(TerminalExecForm form);
}
