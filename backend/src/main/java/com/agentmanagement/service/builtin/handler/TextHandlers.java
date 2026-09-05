package com.agentmanagement.service.builtin.handler;

import com.agentmanagement.service.builtin.BuiltinToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.agentmanagement.service.builtin.handler.Params.str;
import static com.agentmanagement.service.builtin.handler.Params.abbreviate;

/**
 * 文本与编码类内置工具 handler（纯静态，无状态）。
 * hash_calculator 支持文本输入或沙箱文件路径（路径解析由调用方传入沙箱根）。
 */
public final class TextHandlers {

    private TextHandlers() {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** text_stats：字数统计 */
    public static BuiltinToolResult textStats(Map<String, Object> params) {
        String text = str(params.get("text"));
        if (text.isEmpty()) {
            return BuiltinToolResult.fail("text 不能为空");
        }
        int chinese = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.UnicodeScript.of(text.charAt(i)) == Character.UnicodeScript.HAN) {
                chinese++;
            }
        }
        String[] words = text.trim().isEmpty() ? new String[0]
                : text.trim().split("[^A-Za-z0-9']+");
        int wordCount = 0;
        for (String w : words) {
            if (!w.isEmpty()) {
                wordCount++;
            }
        }
        int lines = text.split("\r\n|\n|\r").length;
        return BuiltinToolResult.ok(String.format(
                "总字符数: %d%n中文字符: %d%n英文单词: %d%n行数: %d%nUTF-8 字节: %d",
                text.length(), chinese, wordCount, lines, text.getBytes(StandardCharsets.UTF_8).length));
    }

    /** text_transform：单一变换 op：upper/lower/trim/reverse/unique/sort/number */
    public static BuiltinToolResult textTransform(Map<String, Object> params) {
        String text = str(params.get("text"));
        String op = str(params.get("op")).toLowerCase();
        if (text.isEmpty() || op.isEmpty()) {
            return BuiltinToolResult.fail("参数需要 text 与 op（upper/lower/trim/reverse/unique/sort/number）");
        }
        switch (op) {
            case "upper":
                return BuiltinToolResult.ok(text.toUpperCase());
            case "lower":
                return BuiltinToolResult.ok(text.toLowerCase());
            case "trim":
                return BuiltinToolResult.ok(text.trim());
            case "reverse":
                return BuiltinToolResult.ok(new StringBuilder(text).reverse().toString());
            case "unique": {
                Set<String> seen = new LinkedHashSet<String>();
                for (String line : text.split("\r\n|\n|\r")) {
                    seen.add(line);
                }
                return BuiltinToolResult.ok(String.join("\n", seen));
            }
            case "sort": {
                Set<String> sorted = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
                for (String line : text.split("\r\n|\n|\r")) {
                    sorted.add(line);
                }
                return BuiltinToolResult.ok(String.join("\n", sorted));
            }
            case "number": {
                String[] lines = text.split("\r\n|\n|\r");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < lines.length; i++) {
                    sb.append(i + 1).append(". ").append(lines[i]);
                    if (i < lines.length - 1) {
                        sb.append('\n');
                    }
                }
                return BuiltinToolResult.ok(sb.toString());
            }
            default:
                return BuiltinToolResult.fail("未知 op: " + op + "（支持 upper/lower/trim/reverse/unique/sort/number）");
        }
    }

    /** regex_tool：find（列匹配）/match（校验）/replace（替换） */
    public static BuiltinToolResult regexTool(Map<String, Object> params) {
        String text = str(params.get("text"));
        String pattern = str(params.get("pattern"));
        String mode = str(params.get("mode"));
        String replacement = str(params.get("replacement"));
        if (text.isEmpty() || pattern.isEmpty()) {
            return BuiltinToolResult.fail("参数需要 text 与 pattern");
        }
        if (mode.isEmpty()) {
            mode = "find";
        }
        try {
            Pattern p = Pattern.compile(pattern);
            if ("match".equals(mode)) {
                boolean full = p.matcher(text).matches();
                boolean part = p.matcher(text).find();
                return BuiltinToolResult.ok("完整匹配: " + full + "；包含匹配: " + part);
            }
            if ("replace".equals(mode)) {
                return BuiltinToolResult.ok(p.matcher(text).replaceAll(replacement));
            }
            Matcher m = p.matcher(text);
            StringBuilder sb = new StringBuilder();
            int count = 0;
            while (m.find() && count < 100) {
                sb.append(++count).append(". [").append(m.group()).append(']');
                if (m.groupCount() > 0) {
                    sb.append(" 组:");
                    for (int g = 1; g <= m.groupCount(); g++) {
                        sb.append(' ').append(g).append('=').append(m.group(g));
                    }
                }
                sb.append("（位置 ").append(m.start()).append('-').append(m.end()).append("）\n");
            }
            if (count == 0) {
                return BuiltinToolResult.ok("无匹配");
            }
            sb.append("共 ").append(count).append(" 处匹配");
            return BuiltinToolResult.ok(sb.toString());
        } catch (PatternSyntaxException e) {
            return BuiltinToolResult.fail("正则语法错误: " + e.getDescription());
        }
    }

    /** base64_codec：encode/decode（支持 URL-safe） */
    public static BuiltinToolResult base64Codec(Map<String, Object> params) {
        String text = str(params.get("text"));
        String action = str(params.get("action"));
        boolean urlSafe = Params.boolOf(params.get("url_safe"));
        if (text.isEmpty() || action.isEmpty()) {
            return BuiltinToolResult.fail("参数需要 text 与 action（encode/decode）");
        }
        Base64.Decoder dec = urlSafe ? Base64.getUrlDecoder() : Base64.getDecoder();
        Base64.Encoder enc = urlSafe ? Base64.getUrlEncoder().withoutPadding() : Base64.getEncoder();
        try {
            if ("decode".equals(action)) {
                return BuiltinToolResult.ok(new String(dec.decode(text), StandardCharsets.UTF_8));
            }
            return BuiltinToolResult.ok(enc.encodeToString(text.getBytes(StandardCharsets.UTF_8)));
        } catch (IllegalArgumentException e) {
            return BuiltinToolResult.fail("Base64 解码失败: " + e.getMessage());
        }
    }

    /** hash_calculator：MD5/SHA-1/SHA-256，输入文本或沙箱文件（path） */
    public static BuiltinToolResult hashCalculator(Map<String, Object> params, Path root, boolean outsideSandbox) throws Exception {
        String algo = str(params.get("algorithm")).toLowerCase();
        String text = str(params.get("text"));
        String path = str(params.get("path"));
        if (!"md5".equals(algo) && !"sha1".equals(algo) && !"sha256".equals(algo) && !"sha-256".equals(algo)) {
            return BuiltinToolResult.fail("algorithm 仅支持 md5/sha1/sha256");
        }
        MessageDigest md = MessageDigest.getInstance("sha-256".equals(algo) ? "SHA-256" : algo.toUpperCase());
        byte[] input;
        String source;
        if (!path.isEmpty()) {
            Path file = FileHandlers.resolveSafe(root, path, outsideSandbox);
            if (!Files.exists(file) || Files.isDirectory(file)) {
                return BuiltinToolResult.fail("文件不存在或是目录: " + path);
            }
            if (Files.size(file) > 10 * 1024 * 1024) {
                return BuiltinToolResult.fail("文件超过 10MB，仅支持小文件哈希");
            }
            input = Files.readAllBytes(file);
            source = "文件 " + path;
        } else if (!text.isEmpty()) {
            input = text.getBytes(StandardCharsets.UTF_8);
            source = "文本";
        } else {
            return BuiltinToolResult.fail("需要 text 或 path 参数");
        }
        byte[] digest = md.digest(input);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return BuiltinToolResult.ok(algo + " (" + source + ") = " + sb);
    }

    /** url_codec：URL 百分号编解码（UTF-8） */
    public static BuiltinToolResult urlCodec(Map<String, Object> params) {
        String text = str(params.get("text"));
        String action = str(params.get("action"));
        if (text.isEmpty() || action.isEmpty()) {
            return BuiltinToolResult.fail("参数需要 text 与 action（encode/decode）");
        }
        try {
            if ("decode".equals(action)) {
                return BuiltinToolResult.ok(java.net.URLDecoder.decode(text, "UTF-8"));
            }
            return BuiltinToolResult.ok(java.net.URLEncoder.encode(text, "UTF-8"));
        } catch (Exception e) {
            return BuiltinToolResult.fail("URL 编解码失败: " + e.getMessage());
        }
    }

    /** json_tool：validate/format/minify/get（点路径取值，如 a.b[0].c） */
    public static BuiltinToolResult jsonTool(Map<String, Object> params) {
        String text = str(params.get("text"));
        String action = str(params.get("action"));
        String path = str(params.get("path"));
        if (text.isEmpty()) {
            return BuiltinToolResult.fail("text 不能为空");
        }
        if (action.isEmpty()) {
            action = "format";
        }
        Object root;
        try {
            root = MAPPER.readValue(text, Object.class);
        } catch (Exception e) {
            return BuiltinToolResult.fail("JSON 解析失败: " + abbreviate(e.getMessage(), 300));
        }
        switch (action) {
            case "validate":
                return BuiltinToolResult.ok("JSON 合法 ✓（顶层类型: " + typeName(root) + "）");
            case "minify":
                return minify(text);
            case "get": {
                if (path.isEmpty()) {
                    return BuiltinToolResult.fail("get 模式需要 path 参数（点路径，如 user.name 或 items[0].id）");
                }
                Object cur = root;
                for (String seg : path.replace("]", "").split("\\." )) {
                    for (String part : seg.split("\\[")) {
                        if (part.isEmpty()) {
                            continue;
                        }
                        try {
                            int idx = Integer.parseInt(part);
                            cur = ((List<?>) cur).get(idx);
                        } catch (NumberFormatException e) {
                            cur = ((Map<?, ?>) cur).get(part);
                        } catch (ClassCastException | IndexOutOfBoundsException e) {
                            return BuiltinToolResult.fail("路径不存在: " + path + "（在 " + part + " 处中断）");
                        }
                        if (cur == null) {
                            return BuiltinToolResult.fail("路径不存在: " + path + "（在 " + part + " 处为 null）");
                        }
                    }
                }
                return BuiltinToolResult.ok(cur instanceof String ? (String) cur : writeJson(cur));
            }
            case "format":
            default:
                return BuiltinToolResult.ok(writeJson(root));
        }
    }

    private static BuiltinToolResult minify(String text) {
        try {
            Object o = MAPPER.readValue(text, Object.class);
            return BuiltinToolResult.ok(MAPPER.writeValueAsString(o));
        } catch (Exception e) {
            return BuiltinToolResult.fail("JSON 解析失败: " + e.getMessage());
        }
    }

    private static String writeJson(Object o) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    private static String typeName(Object o) {
        if (o instanceof Map) {
            return "object";
        }
        if (o instanceof List) {
            return "array";
        }
        return o == null ? "null" : o.getClass().getSimpleName();
    }

    /** csv_json_convert：csv_to_json / json_to_csv（支持引号包裹与转义） */
    public static BuiltinToolResult csvJsonConvert(Map<String, Object> params) {
        String text = str(params.get("text"));
        String direction = str(params.get("direction"));
        String delimiter = str(params.get("delimiter"));
        if (text.isEmpty() || direction.isEmpty()) {
            return BuiltinToolResult.fail("参数需要 text 与 direction（csv_to_json/json_to_csv）");
        }
        if (delimiter.isEmpty()) {
            delimiter = ",";
        }
        try {
            if ("json_to_csv".equals(direction)) {
                List<?> rows = MAPPER.readValue(text, List.class);
                if (rows.isEmpty()) {
                    return BuiltinToolResult.fail("JSON 数组为空");
                }
                Set<String> headers = new LinkedHashSet<String>();
                for (Object row : rows) {
                    if (row instanceof Map) {
                        for (Object k : ((Map<?, ?>) row).keySet()) {
                            headers.add(String.valueOf(k));
                        }
                    }
                }
                StringBuilder sb = new StringBuilder();
                sb.append(csvLine(headers, delimiter));
                for (Object row : rows) {
                    List<String> cells = new ArrayList<String>();
                    for (String h : headers) {
                        Object v = row instanceof Map ? ((Map<?, ?>) row).get(h) : null;
                        cells.add(v == null ? "" : String.valueOf(v));
                    }
                    sb.append('\n').append(csvLine(cells, delimiter));
                }
                return BuiltinToolResult.ok(sb.toString());
            }
            List<String[]> rows = parseCsv(text, delimiter.charAt(0));
            if (rows.size() < 2) {
                return BuiltinToolResult.fail("CSV 至少需要表头行 + 1 行数据");
            }
            String[] headers = rows.get(0);
            List<Map<String, String>> out = new ArrayList<Map<String, String>>();
            for (int i = 1; i < rows.size(); i++) {
                Map<String, String> row = new java.util.LinkedHashMap<String, String>();
                for (int c = 0; c < headers.length; c++) {
                    row.put(headers[c], c < rows.get(i).length ? rows.get(i)[c] : "");
                }
                out.add(row);
            }
            return BuiltinToolResult.ok(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(out));
        } catch (Exception e) {
            return BuiltinToolResult.fail("转换失败: " + abbreviate(e.getMessage(), 300));
        }
    }

    /** CSV 单行序列化：含分隔符/引号/换行的字段加引号 */
    private static String csvLine(Iterable<String> cells, String delimiter) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String c : cells) {
            if (!first) {
                sb.append(delimiter);
            }
            first = false;
            if (c != null && (c.contains(delimiter) || c.contains("\"") || c.contains("\n"))) {
                sb.append('"').append(c.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(c == null ? "" : c);
            }
        }
        return sb.toString();
    }

    /** 简易 CSV 解析状态机：处理引号包裹与 "" 转义 */
    private static List<String[]> parseCsv(String text, char d) {
        List<String[]> rows = new ArrayList<String[]>();
        List<String> cells = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == d) {
                cells.add(cur.toString());
                cur.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                cells.add(cur.toString());
                cur.setLength(0);
                rows.add(cells.toArray(new String[0]));
                cells.clear();
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0 || !cells.isEmpty()) {
            cells.add(cur.toString());
            rows.add(cells.toArray(new String[0]));
        }
        return rows;
    }

    /** text_diff：行级 LCS 对比 */
    public static BuiltinToolResult textDiff(Map<String, Object> params) {
        String t1 = str(params.get("text1"));
        String t2 = str(params.get("text2"));
        List<String> a = linesOf(t1, 1000);
        List<String> b = linesOf(t2, 1000);
        int n = a.size(), m = b.size();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                dp[i][j] = a.get(i).equals(b.get(j)) ? dp[i + 1][j + 1] + 1 : Math.max(dp[i + 1][j], dp[i][j + 1]);
            }
        }
        StringBuilder sb = new StringBuilder();
        int added = 0, removed = 0, same = 0;
        int i = 0, j = 0;
        while (i < n && j < m) {
            if (a.get(i).equals(b.get(j))) {
                same++;
                i++;
                j++;
            } else if (dp[i + 1][j] >= dp[i][j + 1]) {
                sb.append("- ").append(a.get(i++)).append('\n');
                removed++;
            } else {
                sb.append("+ ").append(b.get(j++)).append('\n');
                added++;
            }
        }
        while (i < n) {
            sb.append("- ").append(a.get(i++)).append('\n');
            removed++;
        }
        while (j < m) {
            sb.append("+ ").append(b.get(j++)).append('\n');
            added++;
        }
        String head = String.format("对比完成：相同 %d 行，新增 %d 行，删除 %d 行%n", same, added, removed);
        if (added + removed == 0) {
            return BuiltinToolResult.ok(head + "两段文本完全一致");
        }
        return BuiltinToolResult.ok(head + "----\n" + abbreviate(sb.toString().trim(), 8000));
    }

    private static List<String> linesOf(String text, int cap) {
        List<String> lines = new ArrayList<String>();
        if (text.isEmpty()) {
            return lines;
        }
        for (String line : text.split("\r\n|\n|\r")) {
            if (lines.size() >= cap) {
                break;
            }
            lines.add(line);
        }
        return lines;
    }
}
