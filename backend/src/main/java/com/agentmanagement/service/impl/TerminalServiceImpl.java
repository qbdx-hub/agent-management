package com.agentmanagement.service.impl;

import com.agentmanagement.common.BusinessException;
import com.agentmanagement.common.ResultCode;
import com.agentmanagement.entity.WorkspaceMember;
import com.agentmanagement.form.TerminalExecForm;
import com.agentmanagement.mapper.WorkspaceMemberMapper;
import com.agentmanagement.security.SecurityUtils;
import com.agentmanagement.service.TerminalService;
import com.agentmanagement.util.ShellExec;
import com.agentmanagement.vo.TerminalExecVO;
import com.agentmanagement.vo.TerminalInfoVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * 终端实现。这是平台首个用户直连的命令执行通道（RCE 通道），防线依次为：
 * 成员资格校验 → owner/admin 角色闸 → 每用户单飞闸 → 沙箱路径铁笼 →
 * 30s 硬超时 + 2 万字符输出上限 + 命令长度校验 → @AuditLog 全量审计（含 failure）。
 * 不做命令黑名单（易漏），越界一律拒绝。
 */
@Service
public class TerminalServiceImpl implements TerminalService {

    /** 硬超时：超时 destroyForcibly（孙进程可能残留，见 ShellExec 类注释） */
    private static final long TIMEOUT_MS = 30_000L;

    /** 输出上限字符数 */
    private static final int MAX_OUTPUT_CHARS = 20_000;

    @Autowired
    private WorkspaceMemberMapper workspaceMemberMapper;

    @Value("${agent.sandbox.root:data/agent-workspaces}")
    private String sandboxRoot;

    /** 每用户并发闸：同一用户同时只允许 1 条命令在执行 */
    private final ConcurrentHashMap<Long, Semaphore> singleFlight = new ConcurrentHashMap<Long, Semaphore>();

    @Override
    public TerminalInfoVO info() {
        WorkspaceMember member = requireAdminMember();
        Path root = wsRoot(currentWorkspaceId());
        TerminalInfoVO vo = new TerminalInfoVO();
        vo.setOs(System.getProperty("os.name", ""));
        vo.setRole(member.getRole());
        vo.setSandboxPath(root.getFileName() != null ? root.getFileName().toString() : "ws");
        return vo;
    }

    @Override
    public TerminalExecVO exec(TerminalExecForm form) {
        Long wsId = currentWorkspaceId();
        requireAdminMember();

        Semaphore permit = singleFlight.computeIfAbsent(SecurityUtils.currentUserId(), k -> new Semaphore(1));
        if (!permit.tryAcquire()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "上一条命令仍在执行中，请稍候");
        }
        try {
            return doExec(form, wsId);
        } finally {
            permit.release();
        }
    }

    private TerminalExecVO doExec(TerminalExecForm form, Long wsId) {
        Path root = wsRoot(wsId);
        String command = form.getCommand().trim();

        // ---- cwd 边界校验：normalize 后必须仍在沙箱根内（拒绝绝对路径与 .. 穿越）----
        String rel = form.getCwd() == null ? "" : form.getCwd().trim().replace('\\', '/');
        Path dir = root;
        if (!rel.isEmpty() && !".".equals(rel)) {
            if (Paths.get(rel).isAbsolute()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "cwd 必须是沙箱内相对路径");
            }
            dir = root.resolve(rel).normalize();
            if (!dir.startsWith(root)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "路径越界，仅限沙箱内: " + rel);
            }
            if (!Files.isDirectory(dir)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "工作目录不存在: " + rel + "（输入 cd 返回根目录）");
            }
        }

        // ---- cd 特判：服务端校验并计算新目录，不 spawn 进程 ----
        if ("cd".equals(command)) {
            return vo("已切换到沙箱根", 0, 0L, "", false, false);
        }
        if (command.startsWith("cd ")) {
            return cd(form, root, dir, command.substring(3).trim());
        }

        // ---- 普通命令 ----
        long t0 = System.currentTimeMillis();
        try {
            ShellExec.Result r = ShellExec.exec(command, dir, TIMEOUT_MS, MAX_OUTPUT_CHARS);
            TerminalExecVO result = vo(r.output, r.timedOut ? -1 : r.exitCode,
                    System.currentTimeMillis() - t0, rel, r.truncated, r.timedOut);
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.PARAM_ERROR, "执行被中断");
        } catch (IOException e) {
            // 典型场景：cwd 被之前的命令删除后 start() 失败
            throw new BusinessException(ResultCode.PARAM_ERROR, "命令启动失败: " + e.getMessage() + "（输入 cd 返回根目录）");
        }
    }

    /** cd 处理：仅支持沙箱内单个相对目录参数，越界/不存在一律拒绝（cwd 保持不变） */
    private TerminalExecVO cd(TerminalExecForm form, Path root, Path dir, String rawArg) {
        String arg = rawArg.replaceAll("^\"|\"$", "").trim();
        if (arg.isEmpty() || ".".equals(arg)) {
            return vo("", 0, 0L, relOf(root, dir), false, false);
        }
        if (arg.split("\\s+").length > 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "cd 一次只支持一个目录参数");
        }
        if (Paths.get(arg).isAbsolute()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "cd 仅支持沙箱内相对路径");
        }
        Path target = dir.resolve(arg).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "路径越界，仅限沙箱内: " + arg);
        }
        if (!Files.isDirectory(target)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "目录不存在: " + arg);
        }
        return vo("", 0, 0L, relOf(root, target), false, false);
    }

    // ==================== 辅助 ====================

    /** 当前用户必须是目标空间的成员，且角色为 owner/admin（终端是 RCE 通道，不对普通成员开放） */
    private WorkspaceMember requireAdminMember() {
        WorkspaceMember member = workspaceMemberMapper.selectOne(
                new LambdaQueryWrapper<WorkspaceMember>()
                        .eq(WorkspaceMember::getWorkspaceId, currentWorkspaceId())
                        .eq(WorkspaceMember::getUserId, SecurityUtils.currentUserId())
                        .last("LIMIT 1"));
        if (member == null) {
            // 与 requireMember 口径一致：非成员一律「工作空间不存在」，不泄露空间存在性
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "工作空间不存在");
        }
        String role = member.getRole();
        if (!"owner".equals(role) && !"admin".equals(role)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "终端仅空间所有者/管理员可用");
        }
        return member;
    }

    /** 终端沙箱根：agent.sandbox.root/ws-{workspaceId}（共享工作区层，绝不允许沙箱外） */
    private Path wsRoot(Long wsId) {
        Path root = Paths.get(sandboxRoot, "ws-" + wsId).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "沙箱目录创建失败: " + e.getMessage());
        }
        return root;
    }

    private Long currentWorkspaceId() {
        return SecurityUtils.currentWorkspaceId();
    }

    private String relOf(Path root, Path p) {
        return root.equals(p) ? "" : root.relativize(p).toString().replace('\\', '/');
    }

    private TerminalExecVO vo(String output, Integer exitCode, Long durationMs,
                              String cwd, boolean truncated, boolean timedOut) {
        TerminalExecVO result = new TerminalExecVO();
        result.setOutput(output);
        result.setExitCode(exitCode);
        result.setDurationMs(durationMs);
        result.setCwd(cwd);
        result.setTruncated(truncated);
        result.setTimedOut(timedOut);
        return result;
    }
}
