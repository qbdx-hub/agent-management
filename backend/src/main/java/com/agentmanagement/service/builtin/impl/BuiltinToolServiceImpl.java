package com.agentmanagement.service.builtin.impl;

import com.agentmanagement.entity.Tool;
import com.agentmanagement.entity.ToolCallRecord;
import com.agentmanagement.mapper.ToolCallRecordMapper;
import com.agentmanagement.service.ToolService;
import com.agentmanagement.service.builtin.BuiltinToolResult;
import com.agentmanagement.service.builtin.BuiltinToolService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 内置工具实现。默认会话级沙箱：所有文件/命令操作限制在 agent.sandbox.root/session-{sessionId} 内，
 * 路径规范化后必须仍位于沙箱根之下（防绝对路径/.. 穿越）；命令执行限制工作目录 + 超时 + 输出上限；
 * web_fetch 拒绝内网/环回地址（SSRF 防护）。
 * 用户在聊天框显式授权后（outsideSandbox=true），文件/命令工具切换到服务器真实文件系统：
 * 相对路径以服务器进程目录为基准、绝对路径放行，命令工作目录同为进程目录（超时/输出上限不变）。
 * 已知边界：不解析符号链接真实路径、命令本身仍可访问系统其他路径（进程级沙箱如 Docker 属后续演进），
 * 当前定位为可信内网环境的开发平台。
 */
@Slf4j
@Service
public class BuiltinToolServiceImpl implements BuiltinToolService {

    /** 沙箱根目录（相对后端工作目录），每会话一个子目录 */
    @Value("${agent.sandbox.root:data/agent-workspaces}")
    private String sandboxRoot;

    @Autowired
    private ToolCallRecordMapper toolCallRecordMapper;

    @Autowired
    private ToolService toolService;

    /** 抓取/搜索专用 HTTP 客户端 */
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();

    /** 单次读取/输出的字符上限，防止超大文件或失控命令撑爆上下文 */
    private static final int MAX_READ_CHARS = 64_000;
    private static final int MAX_OUTPUT_CHARS = 32_000;
    private static final long MAX_SCAN_FILE_BYTES = 1024 * 1024;
    private static final int MAX_LIST_ENTRIES = 200;
    private static final int MAX_GREP_MATCHES = 100;
    /** 搜索/列举时跳过的目录名 */
    private static final List<String> SKIP_DIRS = new ArrayList<String>();

    static {
        SKIP_DIRS.add(".git");
        SKIP_DIRS.add("node_modules");
        SKIP_DIRS.add("target");
        SKIP_DIRS.add("__pycache__");
        SKIP_DIRS.add(".idea");
    }

    // ==================== 分派入口 ====================

    @Override
    public BuiltinToolResult execute(Tool tool, Map<String, Object> params,
                                     Long agentId, Long sessionId, Long workspaceId) {
        return execute(tool, params, agentId, sessionId, workspaceId, false);
    }

    @Override
    public BuiltinToolResult execute(Tool tool, Map<String, Object> params,
                                     Long agentId, Long sessionId, Long workspaceId, boolean outsideSandbox) {
        long startMs = System.currentTimeMillis();
        BuiltinToolResult result;
        try {
            String name = tool.getName();
            switch (name) {
                case "read_file":
                    result = readFile(params, sessionId, outsideSandbox);
                    break;
                case "write_file":
                    result = writeFile(params, sessionId, outsideSandbox);
                    break;
                case "edit_file":
                    result = editFile(params, sessionId, outsideSandbox);
                    break;
                case "list_files":
                    result = listFiles(params, sessionId, outsideSandbox);
                    break;
                case "search_files":
                    result = searchFiles(params, sessionId, outsideSandbox);
                    break;
                case "run_command":
                    result = runCommand(params, sessionId, outsideSandbox);
                    break;
                case "web_search":
                    result = webSearch(params);
                    break;
                case "web_fetch":
                    result = webFetch(params);
                    break;
                default:
                    result = BuiltinToolResult.fail("未知内置工具: " + name);
            }
        } catch (Exception e) {
            log.warn("内置工具执行异常: tool={}, sessionId={}", tool.getName(), sessionId, e);
            result = BuiltinToolResult.fail(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
        int latencyMs = (int) (System.currentTimeMillis() - startMs);

        // 与 API 工具同一套归因与统计：tool_call_record 落库 + 工具市场统计回填
        recordCall(tool, params, agentId, sessionId, result, latencyMs);
        return result;
    }

    // ==================== 文件工具 ====================

    /** read_file：按行读取文本文件（沙箱内或授权后的真实文件系统），支持 offset/limit 分段读取大文件 */
    private BuiltinToolResult readFile(Map<String, Object> params, Long sessionId, boolean outsideSandbox) throws IOException {
        Path root = workDir(sessionId, outsideSandbox);
        Path target = resolveSafe(root, str(params.get("path")), outsideSandbox);
        if (!Files.exists(target)) {
            return BuiltinToolResult.fail("文件不存在: " + str(params.get("path")) + "（可先用 list_files 查看目录）");
        }
        if (Files.isDirectory(target)) {
            return BuiltinToolResult.fail("目标是目录不是文件: " + str(params.get("path")));
        }
        long size = Files.size(target);
        byte[] bytes = readCapped(target, 2 * MAX_READ_CHARS);
        if (isBinary(bytes)) {
            return BuiltinToolResult.fail("二进制文件（" + size + " 字节），无法以文本读取");
        }
        List<String> lines = splitLines(new String(bytes, StandardCharsets.UTF_8));
        int offset = intOf(params.get("offset"), 1);
        int limit = intOf(params.get("limit"), 400);
        int from = Math.max(0, offset - 1);
        int to = Math.min(lines.size(), from + limit);
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            sb.append(lines.get(i)).append('\n');
            if (sb.length() > MAX_READ_CHARS) {
                sb.append("…（达到单次读取上限 ").append(MAX_READ_CHARS).append(" 字符，请用 offset/limit 分段读取）");
                return BuiltinToolResult.ok(sb.toString());
            }
        }
        sb.insert(0, "[文件] " + str(params.get("path")) + "  共 " + lines.size() + " 行 / " + size + " 字节，"
                + (lines.isEmpty() ? "空文件" : "第 " + (from + 1) + "-" + to + " 行") + "：\n");
        if (to < lines.size()) {
            sb.append("…（还有 ").append(lines.size() - to).append(" 行未显示，用 offset=").append(to + 1).append(" 继续读）");
        }
        return BuiltinToolResult.ok(sb.toString());
    }

    /** write_file：写入/覆盖文本文件（UTF-8），自动创建父目录 */
    private BuiltinToolResult writeFile(Map<String, Object> params, Long sessionId, boolean outsideSandbox) throws IOException {
        Path root = workDir(sessionId, outsideSandbox);
        String rel = str(params.get("path"));
        Object contentObj = params.get("content");
        String content = contentObj != null ? contentObj.toString() : "";
        Path target = resolveSafe(root, rel, outsideSandbox);
        if (Files.isDirectory(target)) {
            return BuiltinToolResult.fail("目标是目录不能写入: " + rel);
        }
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        Files.write(target, bytes,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        return BuiltinToolResult.ok("已写入 " + rel + "（" + bytes.length + " 字节，"
                + splitLines(content).size() + " 行）");
    }

    /** edit_file：精确字符串替换式编辑，old_string 必须唯一（或显式 replace_all） */
    private BuiltinToolResult editFile(Map<String, Object> params, Long sessionId, boolean outsideSandbox) throws IOException {
        Path root = workDir(sessionId, outsideSandbox);
        String rel = str(params.get("path"));
        Path target = resolveSafe(root, rel, outsideSandbox);
        if (!Files.exists(target) || Files.isDirectory(target)) {
            return BuiltinToolResult.fail("文件不存在或是目录: " + rel + "（先用 read_file 确认内容）");
        }
        String oldStr = str(params.get("old_string"));
        String newStr = params.get("new_string") != null ? params.get("new_string").toString() : "";
        if (oldStr.isEmpty()) {
            return BuiltinToolResult.fail("old_string 不能为空（清空文件请用 write_file 传空 content）");
        }
        byte[] bytes = readCapped(target, 4 * MAX_READ_CHARS);
        String content = new String(bytes, StandardCharsets.UTF_8);
        int occurrences = countOccurrences(content, oldStr);
        if (occurrences == 0) {
            return BuiltinToolResult.fail("未找到待替换文本（出现 0 次）。read_file 确认精确内容（含缩进与换行）后重试");
        }
        boolean replaceAll = boolOf(params.get("replace_all"));
        if (occurrences > 1 && !replaceAll) {
            return BuiltinToolResult.fail("old_string 出现 " + occurrences + " 处：请提供更长的上下文使其唯一，或传 replace_all=true 全部替换");
        }
        String updated = replaceAll
                ? content.replace(oldStr, newStr)
                : content.replaceFirst(Pattern.quote(oldStr), java.util.regex.Matcher.quoteReplacement(newStr));
        Files.write(target, updated.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        int replaced = replaceAll ? occurrences : 1;
        return BuiltinToolResult.ok("已编辑 " + rel + "（替换 " + replaced + " 处）");
    }

    /** list_files：glob 模式列举文件，如 **\/*.java、*.txt */
    private BuiltinToolResult listFiles(Map<String, Object> params, Long sessionId, boolean outsideSandbox) throws IOException {
        Path root = workDir(sessionId, outsideSandbox);
        String base = params.get("path") != null ? str(params.get("path")) : ".";
        Path baseDir = resolveSafe(root, base, outsideSandbox);
        if (!Files.isDirectory(baseDir)) {
            return BuiltinToolResult.fail("基准路径不是目录: " + base);
        }
        String pattern = params.get("pattern") != null && !str(params.get("pattern")).isEmpty()
                ? str(params.get("pattern")) : "**/*";
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        List<String> entries = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(baseDir)) {
            walk.filter(p -> !isSkippedDir(p, baseDir))
                    .forEach(p -> {
                        String relPath = baseDir.relativize(p).toString().replace('\\', '/');
                        if (relPath.isEmpty()) {
                            return;
                        }
                        boolean match = matcher.matches(Paths.get(relPath)) || matcher.matches(p.getFileName());
                        if (match && entries.size() < MAX_LIST_ENTRIES) {
                            entries.add(Files.isDirectory(p) ? relPath + "/" : relPath);
                        }
                    });
        }
        if (entries.isEmpty()) {
            return BuiltinToolResult.ok("目录 " + base + " 下没有匹配 " + pattern + " 的文件（沙箱可能为空，先 write_file 创建）");
        }
        entries.sort(Comparator.naturalOrder());
        StringBuilder sb = new StringBuilder("共 ").append(entries.size()).append(" 项：\n");
        for (String e : entries) {
            sb.append(e).append('\n');
        }
        if (entries.size() >= MAX_LIST_ENTRIES) {
            sb.append("…（已达 ").append(MAX_LIST_ENTRIES).append(" 项上限，用更精确的 pattern 过滤）");
        }
        return BuiltinToolResult.ok(sb.toString());
    }

    /** search_files：在文本文件中按内容搜索（Grep），支持正则 */
    private BuiltinToolResult searchFiles(Map<String, Object> params, Long sessionId, boolean outsideSandbox) throws IOException {
        Path root = workDir(sessionId, outsideSandbox);
        String keyword = str(params.get("pattern"));
        if (keyword.isEmpty()) {
            return BuiltinToolResult.fail("pattern 不能为空");
        }
        String base = params.get("path") != null ? str(params.get("path")) : ".";
        Path baseDir = resolveSafe(root, base, outsideSandbox);
        if (!Files.isDirectory(baseDir)) {
            return BuiltinToolResult.fail("基准路径不是目录: " + base);
        }
        boolean regex = boolOf(params.get("is_regex"));
        int max = Math.min(intOf(params.get("max_results"), 50), MAX_GREP_MATCHES);
        Pattern p = regex ? Pattern.compile(keyword) : Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE);

        List<String> matches = new ArrayList<>();
        int scanned = 0;
        int total = 0;
        try (Stream<Path> walk = Files.walk(baseDir)) {
            for (Path file : (Iterable<Path>) walk.filter(Files::isRegularFile)
                    .filter(f -> !isSkippedDir(f, baseDir))::iterator) {
                if (Files.size(file) > MAX_SCAN_FILE_BYTES || matches.size() >= max) {
                    continue;
                }
                byte[] bytes = readCapped(file, MAX_SCAN_FILE_BYTES);
                if (isBinary(bytes)) {
                    continue;
                }
                scanned++;
                List<String> lines = splitLines(new String(bytes, StandardCharsets.UTF_8));
                String relPath = baseDir.relativize(file).toString().replace('\\', '/');
                for (int i = 0; i < lines.size(); i++) {
                    if (p.matcher(lines.get(i)).find()) {
                        total++;
                        if (matches.size() < max) {
                            matches.add(relPath + ":" + (i + 1) + ": " + abbreviate(lines.get(i).trim(), 200));
                        }
                    }
                }
            }
        }
        if (matches.isEmpty()) {
            return BuiltinToolResult.ok("在 " + base + " 下扫描 " + scanned + " 个文本文件，未匹配到: " + keyword);
        }
        StringBuilder sb = new StringBuilder();
        for (String m : matches) {
            sb.append(m).append('\n');
        }
        sb.append("（共 ").append(total).append(" 处匹配，显示前 ").append(matches.size()).append(" 处）");
        return BuiltinToolResult.ok(sb.toString());
    }

    // ==================== 命令执行 ====================

    /** run_command：执行 shell 命令（Windows→cmd /c，Linux→sh -c），工作目录按授权切换，超时强制终止 */
    private BuiltinToolResult runCommand(Map<String, Object> params, Long sessionId, boolean outsideSandbox) throws IOException, InterruptedException {
        String command = str(params.get("command"));
        if (command.isEmpty()) {
            return BuiltinToolResult.fail("command 不能为空");
        }
        int timeoutMs = Math.min(intOf(params.get("timeout_ms"), 30_000), 120_000);
        Path root = workDir(sessionId, outsideSandbox);

        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        ProcessBuilder pb = windows
                ? new ProcessBuilder("cmd.exe", "/c", command)
                : new ProcessBuilder("/bin/sh", "-c", command);
        pb.directory(root.toFile());
        pb.redirectErrorStream(true);

        StringBuilder out = new StringBuilder();
        Process process = pb.start();
        // 子进程输出按系统默认字符集解码（中文 Windows 的 cmd.exe 输出 GBK，按 UTF-8 会乱码）
        java.nio.charset.Charset outputCharset = java.nio.charset.Charset.defaultCharset();
        Thread drainer = new Thread(() -> {
            try (InputStream in = process.getInputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) != -1) {
                    if (out.length() < MAX_OUTPUT_CHARS) {
                        out.append(new String(buf, 0, n, outputCharset));
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
            return BuiltinToolResult.fail("命令超过 " + timeoutMs + "ms 已强制终止。已捕获输出：\n" + abbreviate(out.toString(), 2000));
        }
        drainer.join(2000);
        String output = abbreviate(out.toString().trim(), MAX_OUTPUT_CHARS);
        return BuiltinToolResult.ok("exit=" + process.exitValue() + "\n" + (output.isEmpty() ? "（无输出）" : output));
    }

    // ==================== Web 工具 ====================

    /** web_search：必应 RSS 搜索（cn.bing.com，无需密钥，国内可达） */
    private BuiltinToolResult webSearch(Map<String, Object> params) throws Exception {
        String query = str(params.get("query"));
        if (query.isEmpty()) {
            return BuiltinToolResult.fail("query 不能为空");
        }
        int count = Math.min(Math.max(intOf(params.get("count"), 5), 1), 10);
        String url = "https://cn.bing.com/search?format=rss&count=20&q="
                + URLEncoder.encode(query, "UTF-8");
        String xml = httpGet(url);
        if (xml == null) {
            return BuiltinToolResult.fail("搜索服务暂时不可用，请稍后重试");
        }

        // XXE 防护：禁用 DOCTYPE 与外部实体
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);
        NodeList items = dbf.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                .getElementsByTagName("item");

        if (items.getLength() == 0) {
            return BuiltinToolResult.ok("未搜索到与「" + query + "」相关的结果");
        }
        StringBuilder sb = new StringBuilder("「").append(query).append("」的搜索结果：\n");
        for (int i = 0; i < items.getLength() && i < count; i++) {
            Element item = (Element) items.item(i);
            String title = textOf(item, "title");
            String link = textOf(item, "link");
            String desc = textOf(item, "description");
            sb.append('\n').append(i + 1).append(". ").append(title).append('\n')
                    .append("   ").append(link).append('\n');
            if (!desc.isEmpty()) {
                sb.append("   ").append(abbreviate(desc.replaceAll("\\s+", " "), 200)).append('\n');
            }
        }
        return BuiltinToolResult.ok(sb.toString());
    }

    /** web_fetch：抓取公网 URL 并提取正文文本；拒绝内网/环回地址（SSRF 防护） */
    private BuiltinToolResult webFetch(Map<String, Object> params) throws Exception {
        String url = str(params.get("url"));
        if (url.isEmpty()) {
            return BuiltinToolResult.fail("url 不能为空");
        }
        URI uri = new URI(url);
        String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase() : "";
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            return BuiltinToolResult.fail("仅支持 http/https 地址");
        }
        // SSRF 防护：解析出的所有地址都不得是环回/内网/链路本地
        InetAddress[] addrs = InetAddress.getAllByName(uri.getHost());
        for (InetAddress addr : addrs) {
            if (addr.isLoopbackAddress() || addr.isSiteLocalAddress()
                    || addr.isLinkLocalAddress() || addr.isAnyLocalAddress()) {
                return BuiltinToolResult.fail("不允许访问内网/本机地址: " + addr.getHostAddress());
            }
        }
        int maxLen = Math.min(intOf(params.get("max_length"), 8000), 20_000);

        String contentType;
        String body;
        Request request = new Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0 (AgentManagement bot)")
                .header("Accept", "text/html,application/json,text/plain,*/*")
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return BuiltinToolResult.fail("HTTP " + response.code() + "，抓取失败");
            }
            contentType = response.header("Content-Type", "");
            byte[] bytes = readStreamCapped(response.body() != null ? response.body().byteStream() : null, 2_000_000);
            body = new String(bytes, StandardCharsets.UTF_8);
        }
        if (contentType.contains("application/json") || contentType.contains("xml")
                || contentType.startsWith("text/plain")) {
            return BuiltinToolResult.ok(abbreviate(body.trim(), maxLen));
        }
        if (!contentType.startsWith("text/html")) {
            return BuiltinToolResult.fail("不支持的内容类型: " + contentType + "（仅支持网页/JSON/纯文本）");
        }
        return BuiltinToolResult.ok(abbreviate(htmlToText(body), maxLen));
    }

    // ==================== 沙箱与通用辅助 ====================

    /** 会话沙箱目录：agent.sandbox.root/session-{sessionId}，自动创建 */
    private Path sandboxDir(Long sessionId) throws IOException {
        Path root = Paths.get(sandboxRoot, "session-" + sessionId).toAbsolutePath().normalize();
        Files.createDirectories(root);
        return root;
    }

    /**
     * 本次调用的文件/命令工作目录：默认会话沙箱；用户授权沙箱外运行时为服务器进程目录
     * （相对路径以此为基准、绝对路径放行，真实文件系统可见）。
     */
    private Path workDir(Long sessionId, boolean outsideSandbox) throws IOException {
        return outsideSandbox
                ? Paths.get("").toAbsolutePath().normalize()
                : sandboxDir(sessionId);
    }

    /**
     * 路径解析。沙箱模式：规范化后必须仍在沙箱根内，拒绝绝对路径与 .. 穿越；
     * 沙箱外模式（用户已授权）：Path.resolve 天然支持绝对参数，直接返回规范化结果。
     */
    private Path resolveSafe(Path root, String rel, boolean outsideSandbox) {
        if (rel == null || rel.trim().isEmpty()) {
            throw new IllegalArgumentException("path 不能为空");
        }
        Path resolved = root.resolve(rel).normalize();
        if (!outsideSandbox && !resolved.startsWith(root)) {
            throw new IllegalArgumentException("路径越界，只允许访问会话沙箱内（可在聊天框开启「沙箱外运行」授权）: " + rel);
        }
        return resolved;
    }

    /** 是否跳过该路径（.git/node_modules 等噪声目录） */
    private boolean isSkippedDir(Path p, Path base) {
        for (Path part : base.relativize(p)) {
            if (SKIP_DIRS.contains(part.toString())) {
                return true;
            }
        }
        return false;
    }

    /** 读文件并截断到 maxBytes */
    private byte[] readCapped(Path file, long maxBytes) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return readStreamCapped(in, maxBytes);
        }
    }

    private byte[] readStreamCapped(InputStream in, long maxBytes) throws IOException {
        if (in == null) {
            return new byte[0];
        }
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        long total = 0;
        int n;
        while ((n = in.read(buf)) != -1 && total < maxBytes) {
            bos.write(buf, 0, n);
            total += n;
        }
        return bos.toByteArray();
    }

    /** 二进制探测：前 8KB 出现 NUL 字节即视为二进制 */
    private boolean isBinary(byte[] bytes) {
        int scan = (int) Math.min(bytes.length, 8192);
        for (int i = 0; i < scan; i++) {
            if (bytes[i] == 0) {
                return true;
            }
        }
        return false;
    }

    private List<String> splitLines(String text) {
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\r\n|\n|\r", -1)) {
            if (lines.size() > 20_000) {
                break;
            }
            lines.add(line);
        }
        // split 尾部产生一个空串，文件以换行结尾时去掉
        if (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }

    /** HTML → 纯文本：去 script/style、块级标签转换行、去标签、解码常用实体 */
    private String htmlToText(String html) {
        String t = html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</(p|div|li|h[1-6]|tr)>", "\n")
                .replaceAll("(?s)<[^>]+>", " ")
                .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&mdash;", "—").replace("&hellip;", "…");
        return t.replaceAll("[ \\t]{2,}", " ").replaceAll("\\n{3,}", "\n\n").trim();
    }

    private String httpGet(String url) throws IOException {
        Request request = new Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0 (AgentManagement bot)")
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }
            return new String(readStreamCapped(
                    response.body() != null ? response.body().byteStream() : null, 1_000_000),
                    StandardCharsets.UTF_8);
        }
    }

    private String textOf(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        return nodes.getLength() > 0 && nodes.item(0).getTextContent() != null
                ? nodes.item(0).getTextContent().trim() : "";
    }

    private int countOccurrences(String content, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = content.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    /** 内置工具调用记录：与 API 工具同表归因（tool/agent/session），并回填市场统计 */
    private void recordCall(Tool tool, Map<String, Object> params, Long agentId, Long sessionId,
                            BuiltinToolResult result, int latencyMs) {
        try {
            ToolCallRecord record = new ToolCallRecord();
            record.setToolId(tool.getId());
            record.setAgentId(agentId);
            record.setSessionId(sessionId);
            record.setParams(params);
            record.setSuccess(result.isSuccess() ? 1 : 0);
            record.setLatencyMs(latencyMs);
            String summary = result.isSuccess()
                    ? (result.getOutput() != null ? result.getOutput() : "")
                    : (result.getErrorMessage() != null ? result.getErrorMessage() : "执行失败");
            record.setResultSummary(abbreviate(summary.replaceAll("\\s+", " "), 200));
            if (!result.isSuccess()) {
                record.setErrorMessage(abbreviate(result.getErrorMessage(), 1000));
            }
            record.setCreatedAt(LocalDateTime.now());
            toolCallRecordMapper.insert(record);
            toolService.refreshStatsAfterCall(tool.getId(), result.isSuccess());
        } catch (Exception e) {
            log.warn("内置工具调用记录写入失败: tool={}", tool.getName(), e);
        }
    }

    private String str(Object o) {
        return o != null ? o.toString().trim() : "";
    }

    private int intOf(Object o, int def) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            return o != null ? Integer.parseInt(o.toString().trim()) : def;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private boolean boolOf(Object o) {
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        return o != null && ("true".equalsIgnoreCase(o.toString()) || "1".equals(o.toString()));
    }

    private String abbreviate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
