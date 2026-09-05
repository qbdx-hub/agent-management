package com.agentmanagement.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Shell 命令执行内核（供终端与内置工具 run_command 共用）。
 *
 * <p>安全边界（可信内网环境，与 BuiltinToolServiceImpl 类头声明口径一致）：
 * <ul>
 *   <li>调用方必须已把工作目录限制在沙箱内，本类不做路径校验；</li>
 *   <li>超时 {@code destroyForcibly()} 只杀直接子进程，cmd /c、sh -c 派生的孙进程可能残留
 *       （JDK8 无 ProcessHandle，无法进程组杀死；升级 JDK17 后可用
 *       {@code ProcessHandle.descendants().forEach(ProcessHandle::destroyForcibly)} 根治）；</li>
 *   <li>输出超过 maxChars 后停止追加（置 truncated）但继续消费流，防止管道写满导致进程卡死。</li>
 * </ul>
 */
public final class ShellExec {

    private ShellExec() {
    }

    /** 执行结果。output 已 trim；timedOut 时 exitCode 保持 -1。 */
    public static class Result {
        public String output = "";
        public int exitCode = -1;
        public boolean timedOut;
        public boolean truncated;
    }

    /**
     * 执行 shell 命令（Windows→cmd /c，Linux→/bin/sh -c），stdout+stderr 合并回显，超时强制终止。
     *
     * <p>子进程输出按系统默认字符集解码——中文 Windows 的 cmd.exe 输出 GBK，
     * 按 UTF-8 解会乱码；注意若 JVM 以 -Dfile.encoding=UTF-8 启动而 cmd 仍输出 GBK，
     * 部署侧需保证两者一致。
     *
     * @param command   命令行（原样交给 shell）
     * @param directory 工作目录（调用方保证已存在且在沙箱内）
     * @param timeoutMs 超时毫秒数，超时强制终止并置 timedOut
     * @param maxChars  输出保留的最大字符数，超出部分丢弃并置 truncated
     */
    public static Result exec(String command, Path directory, long timeoutMs, int maxChars)
            throws IOException, InterruptedException {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        ProcessBuilder pb = windows
                ? new ProcessBuilder("cmd.exe", "/c", command)
                : new ProcessBuilder("/bin/sh", "-c", command);
        pb.directory(directory.toFile());
        pb.redirectErrorStream(true);

        Result result = new Result();
        StringBuilder out = new StringBuilder();
        Charset outputCharset = Charset.defaultCharset();
        Process process = pb.start();
        Thread drainer = new Thread(() -> {
            try (InputStream in = process.getInputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) != -1) {
                    if (out.length() < maxChars) {
                        out.append(new String(buf, 0, n, outputCharset));
                    } else {
                        result.truncated = true;
                    }
                }
            } catch (IOException ignored) {
                // 进程被终止时流关闭属正常
            }
        });
        drainer.setDaemon(true);
        drainer.start();

        boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            drainer.join(1000);
            result.timedOut = true;
        } else {
            drainer.join(2000);
            result.exitCode = process.exitValue();
        }
        result.output = abbreviate(out.toString().trim(), maxChars);
        return result;
    }

    private static String abbreviate(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "…（输出过长已截断）";
    }
}
