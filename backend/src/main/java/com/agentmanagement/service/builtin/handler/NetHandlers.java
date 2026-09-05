package com.agentmanagement.service.builtin.handler;

import com.agentmanagement.service.builtin.BuiltinToolResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.agentmanagement.service.builtin.handler.Params.str;
import static com.agentmanagement.service.builtin.handler.Params.intOf;
import static com.agentmanagement.service.builtin.handler.Params.abbreviate;

/**
 * 网络查询与通知类内置工具 handler（纯静态，共享一个 OkHttpClient）。
 * 所有出站请求统一过 {@link #publicUrlGuard}（仅 http/https + 拒绝环回/内网/链路本地地址，SSRF 防护），
 * 响应体读取封顶，防止超大响应拖垮上下文。
 */
public final class NetHandlers {

    private NetHandlers() {
    }

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 响应体读取字节上限（1MB） */
    private static final long MAX_BODY_BYTES = 1024 * 1024;

    // ==================== 共用：SSRF 防护 ====================

    /**
     * 出站 URL 安全校验：仅 http/https；解析出的所有 IP 不得是环回/内网/链路本地/通配地址。
     * 返回 null 表示通过，否则返回错误文案。
     */
    public static String publicUrlGuard(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (Exception e) {
            return "URL 格式错误: " + url;
        }
        String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase() : "";
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            return "仅支持 http/https 地址";
        }
        if (uri.getHost() == null) {
            return "URL 缺少主机名: " + url;
        }
        try {
            InetAddress[] addrs = InetAddress.getAllByName(uri.getHost());
            for (InetAddress addr : addrs) {
                if (addr.isLoopbackAddress() || addr.isSiteLocalAddress()
                        || addr.isLinkLocalAddress() || addr.isAnyLocalAddress()) {
                    return "不允许访问内网/本机地址: " + addr.getHostAddress();
                }
            }
        } catch (Exception e) {
            return "域名解析失败: " + uri.getHost();
        }
        return null;
    }

    /** GET 抓取（已过 guard 的 URL），返回原始 UTF-8 文本（封顶 1MB） */
    private static String httpGetBody(String url) throws Exception {
        Request request = new Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0 (AgentManagement bot)")
                .build();
        try (Response response = HTTP.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("HTTP " + response.code());
            }
            return readBody(response);
        }
    }

    private static String readBody(Response response) throws Exception {
        InputStream in = response.body() != null ? response.body().byteStream() : null;
        if (in == null) {
            return "";
        }
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        long total = 0;
        int n;
        while ((n = in.read(buf)) != -1 && total < MAX_BODY_BYTES) {
            bos.write(buf, 0, n);
            total += n;
        }
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    // ==================== 通用 HTTP ====================

    /** http_request：通用 REST 调用（GET/POST/PUT/DELETE/PATCH） */
    public static BuiltinToolResult httpRequest(Map<String, Object> params) {
        String url = str(params.get("url"));
        String method = str(params.get("method"));
        String body = str(params.get("body"));
        Object headersObj = params.get("headers");
        if (url.isEmpty()) {
            return BuiltinToolResult.fail("url 不能为空");
        }
        if (method.isEmpty()) {
            method = "GET";
        }
        String guard = publicUrlGuard(url);
        if (guard != null) {
            return BuiltinToolResult.fail(guard);
        }
        try {
            Request.Builder rb = new Request.Builder().url(url)
                    .header("User-Agent", "Mozilla/5.0 (AgentManagement bot)");
            if (headersObj instanceof Map) {
                for (Map.Entry<?, ?> h : ((Map<?, ?>) headersObj).entrySet()) {
                    rb.header(String.valueOf(h.getKey()), String.valueOf(h.getValue()));
                }
            } else if (headersObj != null && !str(headersObj).isEmpty()) {
                // "K: V" 每行一个的简化写法
                for (String line : str(headersObj).split("\n")) {
                    int idx = line.indexOf(':');
                    if (idx > 0) {
                        rb.header(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
                    }
                }
            }
            boolean hasBody = !body.isEmpty() && !"GET".equals(method) && !"HEAD".equals(method);
            if (hasBody) {
                rb.method(method, RequestBody.create(body, MediaType.parse("application/json; charset=utf-8")));
            }
            long t0 = System.currentTimeMillis();
            try (Response response = HTTP.newCall(rb.build()).execute()) {
                String respBody = readBody(response);
                long cost = System.currentTimeMillis() - t0;
                StringBuilder sb = new StringBuilder();
                sb.append("HTTP ").append(response.code()).append("（").append(cost).append("ms）\n");
                sb.append("Content-Type: ").append(response.header("Content-Type", "")).append('\n');
                sb.append("----\n");
                sb.append(respBody.isEmpty() ? "（无响应体）" : abbreviate(respBody.trim(), 8000));
                return BuiltinToolResult.ok(sb.toString());
            }
        } catch (Exception e) {
            return BuiltinToolResult.fail("请求失败: " + e.getMessage());
        }
    }

    // ==================== 天气 ====================

    /** WMO weather code → 中文描述 */
    private static String weatherCn(int code) {
        if (code == 0) {
            return "晴";
        }
        if (code <= 3) {
            return new String[]{"", "基本晴", "局部多云", "多云"}[code];
        }
        if (code == 45 || code == 48) {
            return "雾";
        }
        if (code >= 51 && code <= 57) {
            return "毛毛雨";
        }
        if (code >= 61 && code <= 67) {
            return code >= 65 ? "大雨" : "雨";
        }
        if (code >= 71 && code <= 77) {
            return "雪";
        }
        if (code == 80 || code == 81 || code == 82) {
            return "阵雨";
        }
        if (code == 85 || code == 86) {
            return "阵雪";
        }
        if (code >= 95) {
            return "雷阵雨";
        }
        return "未知(" + code + ")";
    }

    /** weather_forecast：open-meteo 免密钥天气（当前 + 未来 N 天预报） */
    public static BuiltinToolResult weatherForecast(Map<String, Object> params) {
        String city = str(params.get("city"));
        int days = Math.min(Math.max(intOf(params.get("days"), 3), 1), 7);
        if (city.isEmpty()) {
            return BuiltinToolResult.fail("city 不能为空，如 北京");
        }
        try {
            // 1) 城市 → 经纬度（open-meteo geocoding，免密钥）
            String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?count=1&language=zh&format=json&name="
                    + URLEncoder.encode(city, "UTF-8");
            JsonNode geo = MAPPER.readTree(httpGetBody(geoUrl)).path("results").path(0);
            if (geo.isMissingNode()) {
                return BuiltinToolResult.fail("未找到城市: " + city);
            }
            double lat = geo.path("latitude").asDouble();
            double lon = geo.path("longitude").asDouble();
            String name = geo.path("name").asText(city);
            String admin = geo.path("admin1").asText("");

            // 2) 预报
            String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                    + "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"
                    + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"
                    + "&timezone=auto&forecast_days=" + days;
            JsonNode r = MAPPER.readTree(httpGetBody(url));
            JsonNode cur = r.path("current");
            JsonNode daily = r.path("daily");
            StringBuilder sb = new StringBuilder();
            sb.append(name).append(admin.isEmpty() ? "" : "（" + admin + "）")
                    .append(" · ").append(r.path("timezone").asText("")).append('\n');
            sb.append(String.format("当前: %.1f°C，%s，湿度 %d%%，风速 %.0f km/h%n",
                    cur.path("temperature_2m").asDouble(), weatherCn(cur.path("weather_code").asInt()),
                    cur.path("relative_humidity_2m").asInt(), cur.path("wind_speed_10m").asDouble()));
            sb.append("---- 未来预报 ----\n");
            for (int i = 0; i < days && i < daily.path("time").size(); i++) {
                sb.append(daily.path("time").path(i).asText())
                        .append(String.format("  %s  %+.0f~%+.0f°C  降水概率 %d%%\n",
                                weatherCn(daily.path("weather_code").path(i).asInt()),
                                daily.path("temperature_2m_min").path(i).asDouble(),
                                daily.path("temperature_2m_max").path(i).asDouble(),
                                daily.path("precipitation_probability_max").path(i).asInt()));
            }
            return BuiltinToolResult.ok(sb.toString().trim());
        } catch (Exception e) {
            return BuiltinToolResult.fail("天气查询失败: " + e.getMessage());
        }
    }

    // ==================== IP / DNS ====================

    /** ip_lookup：IP 归属地（ip-api.com 免密钥中文；不传 ip 查当前出口 IP） */
    public static BuiltinToolResult ipLookup(Map<String, Object> params) {
        String ip = str(params.get("ip"));
        try {
            String url = "http://ip-api.com/json/" + URLEncoder.encode(ip, "UTF-8") + "?lang=zh-CN";
            JsonNode r = MAPPER.readTree(httpGetBody(url));
            if (!"success".equals(r.path("status").asText())) {
                return BuiltinToolResult.fail("查询失败: " + r.path("message").asText("未知原因"));
            }
            return BuiltinToolResult.ok(String.format(
                    "IP: %s%n位置: %s %s %s%n运营商: %s%n时区: %s",
                    r.path("query").asText(),
                    r.path("country").asText(), r.path("regionName").asText(), r.path("city").asText(),
                    r.path("isp").asText(), r.path("timezone").asText()));
        } catch (Exception e) {
            return BuiltinToolResult.fail("IP 查询失败: " + e.getMessage());
        }
    }

    /** dns_lookup：域名解析（JDK InetAddress，列出全部 A 记录） */
    public static BuiltinToolResult dnsLookup(Map<String, Object> params) {
        String host = str(params.get("host"));
        if (host.isEmpty()) {
            return BuiltinToolResult.fail("host 不能为空");
        }
        try {
            InetAddress[] addrs = InetAddress.getAllByName(host);
            StringBuilder sb = new StringBuilder();
            sb.append(host).append(" → ").append(addrs.length).append(" 个地址\n");
            for (InetAddress addr : addrs) {
                sb.append(addr.getHostAddress());
                if (addr.isSiteLocalAddress() || addr.isLoopbackAddress()) {
                    sb.append("（内网）");
                }
                sb.append('\n');
            }
            return BuiltinToolResult.ok(sb.toString().trim());
        } catch (Exception e) {
            return BuiltinToolResult.fail("域名解析失败: " + e.getMessage());
        }
    }

    // ==================== 网页元信息 ====================

    /** url_metadata：抓取网页 title/description/og 标签（正则提取，比 web_fetch 更结构化） */
    public static BuiltinToolResult urlMetadata(Map<String, Object> params) {
        String url = str(params.get("url"));
        if (url.isEmpty()) {
            return BuiltinToolResult.fail("url 不能为空");
        }
        String guard = publicUrlGuard(url);
        if (guard != null) {
            return BuiltinToolResult.fail(guard);
        }
        try {
            String html = httpGetBody(url);
            StringBuilder sb = new StringBuilder();
            sb.append("URL: ").append(url).append('\n');
            String title = firstRegex(html, "(?is)<title[^>]*>(.*?)</title>");
            sb.append("title: ").append(title.isEmpty() ? "（无）" : decodeEntities(title)).append('\n');
            String desc = firstMeta(html, "description");
            sb.append("description: ").append(desc.isEmpty() ? "（无）" : abbreviate(decodeEntities(desc), 300)).append('\n');
            for (String prop : new String[]{"og:title", "og:description", "og:image", "og:site_name"}) {
                String v = firstMeta(html, prop);
                if (!v.isEmpty()) {
                    sb.append(prop).append(": ").append(abbreviate(decodeEntities(v), 200)).append('\n');
                }
            }
            String canonical = firstRegex(html, "(?i)<link[^>]+rel=[\"']canonical[\"'][^>]+href=[\"']([^\"']+)");
            if (!canonical.isEmpty()) {
                sb.append("canonical: ").append(canonical);
            }
            return BuiltinToolResult.ok(sb.toString().trim());
        } catch (Exception e) {
            return BuiltinToolResult.fail("抓取失败: " + e.getMessage());
        }
    }

    private static String firstRegex(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? m.group(1).replaceAll("\\s+", " ").trim() : "";
    }

    private static String firstMeta(String html, String nameOrProp) {
        String v = firstRegex(html, "(?is)<meta[^>]+(?:name|property)=[\"']" + Pattern.quote(nameOrProp)
                + "[\"'][^>]*content=[\"']([^\"']*)");
        if (v.isEmpty()) {
            v = firstRegex(html, "(?is)<meta[^>]+content=[\"']([^\"']*)[\"'][^>]*(?:name|property)=[\"']"
                    + Pattern.quote(nameOrProp) + "[\"']");
        }
        return v;
    }

    private static String decodeEntities(String s) {
        return s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&nbsp;", " ");
    }

    // ==================== 二维码 ====================

    /** qr_generate：内容 → 二维码 SVG 存沙箱（zxing core 编矩阵，自拼 SVG，无图片库依赖） */
    public static BuiltinToolResult qrGenerate(Map<String, Object> params, Path root, boolean outsideSandbox) {
        String content = str(params.get("content"));
        if (content.isEmpty()) {
            return BuiltinToolResult.fail("content 不能为空（文本或 URL）");
        }
        if (content.length() > 1000) {
            return BuiltinToolResult.fail("内容过长（≤1000 字符）");
        }
        String fileName = str(params.get("file_name"));
        if (fileName.isEmpty()) {
            fileName = "qr-" + System.currentTimeMillis() + ".svg";
        }
        if (!fileName.endsWith(".svg")) {
            fileName = fileName + ".svg";
        }
        try {
            Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 2);
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints);
            int n = matrix.getWidth();
            StringBuilder path = new StringBuilder();
            for (int y = 0; y < n; y++) {
                for (int x = 0; x < n; x++) {
                    if (matrix.get(x, y)) {
                        path.append('M').append(x).append(' ').append(y).append("h1v1h-1z");
                    }
                }
            }
            String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + n + "\" height=\"" + n
                    + "\" viewBox=\"0 0 " + n + " " + n + "\" shape-rendering=\"crispEdges\">"
                    + "<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>"
                    + "<path d=\"" + path + "\" fill=\"#000000\"/></svg>";
            Path out = FileHandlers.resolveSafe(root, fileName, outsideSandbox);
            Files.createDirectories(out.getParent() != null ? out.getParent() : root);
            Files.write(out, svg.getBytes(StandardCharsets.UTF_8));
            return BuiltinToolResult.ok("二维码已生成: " + fileName
                    + "（" + n + "x" + n + "，SVG 矢量图）\n内容: " + abbreviate(content, 100));
        } catch (Exception e) {
            return BuiltinToolResult.fail("二维码生成失败: " + e.getMessage());
        }
    }

    // ==================== PDF ====================

    /** pdf_extract_text：pdfbox 提取 PDF 文本（可指定页码范围） */
    public static BuiltinToolResult pdfExtractText(Map<String, Object> params, Path root, boolean outsideSandbox) {
        String path = str(params.get("path"));
        String pages = str(params.get("pages"));
        if (path.isEmpty()) {
            return BuiltinToolResult.fail("path 不能为空（PDF 相对沙箱根路径）");
        }
        Path file;
        try {
            file = FileHandlers.resolveSafe(root, path, outsideSandbox);
        } catch (IllegalArgumentException e) {
            return BuiltinToolResult.fail(e.getMessage());
        }
        if (!Files.exists(file) || Files.isDirectory(file)) {
            return BuiltinToolResult.fail("文件不存在: " + path);
        }
        try (PDDocument doc = PDDocument.load(file.toFile())) {
            int total = doc.getNumberOfPages();
            int start = 1;
            int end = total;
            if (!pages.isEmpty()) {
                String[] parts = pages.split("-");
                try {
                    start = Math.max(1, Integer.parseInt(parts[0].trim()));
                    end = parts.length > 1 ? Math.min(total, Integer.parseInt(parts[1].trim())) : start;
                } catch (NumberFormatException e) {
                    return BuiltinToolResult.fail("pages 格式应为 3 或 1-5");
                }
            }
            if (start > end) {
                return BuiltinToolResult.fail("页码范围无效: " + start + "-" + end + "（共 " + total + " 页）");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(start);
            stripper.setEndPage(end);
            String text = stripper.getText(doc).trim();
            StringBuilder sb = new StringBuilder();
            sb.append("共 ").append(total).append(" 页，已提取 ").append(start).append("-").append(end)
                    .append(" 页，").append(text.length()).append(" 字符\n----\n");
            sb.append(text.isEmpty() ? "（无文本内容，可能是扫描件）" : abbreviate(text, 8000));
            return BuiltinToolResult.ok(sb.toString());
        } catch (Exception e) {
            return BuiltinToolResult.fail("PDF 解析失败: " + e.getMessage());
        }
    }

    // ==================== 通知 ====================

    /** webhook_notify：POST JSON 到用户提供的 webhook（企微/钉钉/飞书机器人等） */
    public static BuiltinToolResult webhookNotify(Map<String, Object> params) {
        String url = str(params.get("url"));
        String content = str(params.get("content"));
        if (url.isEmpty() || content.isEmpty()) {
            return BuiltinToolResult.fail("参数需要 url（机器人 webhook 地址）与 content（要推送的内容）");
        }
        String guard = publicUrlGuard(url);
        if (guard != null) {
            return BuiltinToolResult.fail(guard);
        }
        try {
            String payload;
            try {
                MAPPER.readTree(content);
                payload = content;
            } catch (Exception notJson) {
                Map<String, Object> wrap = new HashMap<String, Object>();
                wrap.put("content", content);
                payload = MAPPER.writeValueAsString(wrap);
            }
            Request request = new Request.Builder().url(url)
                    .post(RequestBody.create(payload, MediaType.parse("application/json; charset=utf-8")))
                    .build();
            try (Response response = HTTP.newCall(request).execute()) {
                String body = abbreviate(readBody(response).trim(), 500);
                String result = "HTTP " + response.code() + (body.isEmpty() ? "" : "\n" + body);
                if (response.isSuccessful()) {
                    return BuiltinToolResult.ok("推送成功\n" + result);
                }
                return BuiltinToolResult.fail("推送失败\n" + result);
            }
        } catch (Exception e) {
            return BuiltinToolResult.fail("推送失败: " + e.getMessage());
        }
    }
}
