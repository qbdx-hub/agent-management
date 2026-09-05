package com.agentmanagement.controller;

import com.agentmanagement.common.Result;
import com.agentmanagement.common.annotation.AuditLog;
import com.agentmanagement.form.TerminalExecForm;
import com.agentmanagement.service.TerminalService;
import com.agentmanagement.vo.TerminalExecVO;
import com.agentmanagement.vo.TerminalInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 移动端命令终端：在当前工作空间沙箱内执行 shell 命令。
 * 仅 owner/admin 可用（校验在 TerminalServiceImpl），每次执行全量审计。
 */
@RestController
@RequestMapping("/terminal")
public class TerminalController {

    @Autowired
    private TerminalService terminalService;

    @GetMapping("/info")
    public Result<TerminalInfoVO> info() {
        return Result.success(terminalService.info());
    }

    @AuditLog(action = "terminal.exec", label = "执行终端命令", resourceType = "terminal")
    @PostMapping("/exec")
    public Result<TerminalExecVO> exec(@Valid @RequestBody TerminalExecForm form) {
        return Result.success(terminalService.exec(form));
    }
}
