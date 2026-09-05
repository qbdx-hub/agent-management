package com.agentmanagement.service.builtin.handler;

import com.agentmanagement.service.builtin.BuiltinToolResult;
import com.agentmanagement.util.ExprParser;
import org.springframework.scheduling.support.CronExpression;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.agentmanagement.service.builtin.handler.Params.str;
import static com.agentmanagement.service.builtin.handler.Params.intOf;
import static com.agentmanagement.service.builtin.handler.Params.dblOf;
import static com.agentmanagement.service.builtin.handler.Params.boolOf;

/**
 * 计算与时间类内置工具 handler（纯静态，无状态）。
 * calculator 的表达式解析走 {@link ExprParser}（白名单词法，防脚本注入）。
 */
public final class CalcHandlers {

    private CalcHandlers() {
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String[] WEEK_CN = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    // ==================== 计算类 ====================

    /** calculator：数学表达式求值 */
    public static BuiltinToolResult calculator(Map<String, Object> params) {
        String expr = str(params.get("expression"));
        if (expr.isEmpty()) {
            return BuiltinToolResult.fail("expression 不能为空，如 (2+3)*sqrt(16)");
        }
        try {
            double v = ExprParser.eval(expr);
            return BuiltinToolResult.ok(expr + " = " + formatNum(v));
        } catch (IllegalArgumentException e) {
            return BuiltinToolResult.fail(e.getMessage());
        }
    }

    /** 单位换算表：单位 → 「基准单位因子」，各表单位名互不重复 */
    private static final Map<String, Map<String, Double>> UNIT_TABLES = new LinkedHashMap<String, Map<String, Double>>();

    static {
        Map<String, Double> len = new LinkedHashMap<String, Double>();
        len.put("mm", 0.001); len.put("cm", 0.01); len.put("m", 1.0); len.put("km", 1000.0);
        len.put("in", 0.0254); len.put("ft", 0.3048); len.put("mi", 1609.344);
        UNIT_TABLES.put("长度(基准 m)", len);

        Map<String, Double> weight = new LinkedHashMap<String, Double>();
        weight.put("mg", 1e-6); weight.put("g", 0.001); weight.put("kg", 1.0); weight.put("t", 1000.0);
        weight.put("lb", 0.45359237); weight.put("oz", 0.0283495231);
        UNIT_TABLES.put("重量(基准 kg)", weight);

        Map<String, Double> area = new LinkedHashMap<String, Double>();
        area.put("m2", 1.0); area.put("km2", 1e6); area.put("ha", 1e4); area.put("mu", 666.6667);
        area.put("ft2", 0.09290304);
        UNIT_TABLES.put("面积(基准 m2)", area);

        Map<String, Double> volume = new LinkedHashMap<String, Double>();
        volume.put("ml", 0.001); volume.put("l", 1.0); volume.put("m3", 1000.0); volume.put("gal", 3.785412);
        UNIT_TABLES.put("体积(基准 l)", volume);

        Map<String, Double> speed = new LinkedHashMap<String, Double>();
        speed.put("mps", 1.0); speed.put("kmh", 1 / 3.6); speed.put("knot", 0.5144444);
        UNIT_TABLES.put("速度(基准 m/s)", speed);

        Map<String, Double> data = new LinkedHashMap<String, Double>();
        data.put("b", 1.0); data.put("kb", 1024.0); data.put("mb", 1048576.0);
        data.put("gb", 1073741824.0); data.put("tb", 1099511627776.0);
        UNIT_TABLES.put("数据大小(基准 b)", data);
    }

    /** unit_convert：单位换算（温度用偏移公式单独处理） */
    public static BuiltinToolResult unitConvert(Map<String, Object> params) {
        double value = dblOf(params.get("value"), Double.NaN);
        String from = str(params.get("from")).toLowerCase();
        String to = str(params.get("to")).toLowerCase();
        if (Double.isNaN(value) || from.isEmpty() || to.isEmpty()) {
            return BuiltinToolResult.fail("参数需要 value/from/to，如 value=100, from=c, to=f");
        }
        // 温度：以摄氏为基准
        if (isTemp(from) && isTemp(to)) {
            double c = toCelsius(value, from);
            return BuiltinToolResult.ok(value + from + " = " + formatNum(fromCelsius(c, to)) + to);
        }
        String[] fromCat = findUnit(from);
        String[] toCat = findUnit(to);
        if (fromCat == null) {
            return BuiltinToolResult.fail("未知单位: " + from + "（支持 " + supportedUnits() + " 及温度 c/f/k）");
        }
        if (toCat == null) {
            return BuiltinToolResult.fail("未知单位: " + to + "（支持 " + supportedUnits() + " 及温度 c/f/k）");
        }
        if (!fromCat[0].equals(toCat[0])) {
            return BuiltinToolResult.fail("单位类别不同，无法换算: " + fromCat[0] + " → " + toCat[0]);
        }
        double base = value * Double.parseDouble(fromCat[1]);
        double out = base / Double.parseDouble(toCat[1]);
        return BuiltinToolResult.ok(value + from + " = " + formatNum(out) + to);
    }

    private static boolean isTemp(String u) {
        return "c".equals(u) || "f".equals(u) || "k".equals(u);
    }

    private static double toCelsius(double v, String u) {
        if ("c".equals(u)) {
            return v;
        }
        if ("f".equals(u)) {
            return (v - 32) * 5 / 9;
        }
        return v - 273.15;
    }

    private static double fromCelsius(double c, String u) {
        if ("c".equals(u)) {
            return c;
        }
        if ("f".equals(u)) {
            return c * 9 / 5 + 32;
        }
        return c + 273.15;
    }

    /** 返回 [类别描述, 因子字符串]；找不到返回 null */
    private static String[] findUnit(String unit) {
        for (Map.Entry<String, Map<String, Double>> e : UNIT_TABLES.entrySet()) {
            Double f = e.getValue().get(unit);
            if (f != null) {
                return new String[]{e.getKey(), f.toString()};
            }
        }
        return null;
    }

    private static String supportedUnits() {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Double> t : UNIT_TABLES.values()) {
            sb.append(String.join("/", t.keySet())).append("、");
        }
        return sb.substring(0, sb.length() - 1);
    }

    /** number_base_convert：2-36 任意进制互转（BigInteger 支持大数） */
    public static BuiltinToolResult numberBaseConvert(Map<String, Object> params) {
        String value = str(params.get("value"));
        int fromBase = intOf(params.get("from_base"), 10);
        int toBase = intOf(params.get("to_base"), 16);
        if (value.isEmpty()) {
            return BuiltinToolResult.fail("value 不能为空");
        }
        if (fromBase < 2 || fromBase > 36 || toBase < 2 || toBase > 36) {
            return BuiltinToolResult.fail("进制仅支持 2-36");
        }
        try {
            BigInteger n = new BigInteger(value, fromBase);
            return BuiltinToolResult.ok(value + " (base-" + fromBase + ") = "
                    + n.toString(toBase).toUpperCase() + " (base-" + toBase + ")");
        } catch (NumberFormatException e) {
            return BuiltinToolResult.fail("值 " + value + " 不是合法的 " + fromBase + " 进制数");
        }
    }

    /** random_generator：随机数 / UUID / 密码 */
    public static BuiltinToolResult randomGenerator(Map<String, Object> params) {
        String type = str(params.get("type"));
        if (type.isEmpty()) {
            type = "number";
        }
        switch (type) {
            case "uuid":
                return BuiltinToolResult.ok(UUID.randomUUID().toString());
            case "password":
                return password(intOf(params.get("length"), 16), boolOf(params.get("uppercase")),
                        boolOf(params.get("lowercase")), boolOf(params.get("digits")), boolOf(params.get("symbols")));
            case "number":
            default:
                long min = Params.longOf(params.get("min"), 0);
                long max = Params.longOf(params.get("max"), 100);
                if (max < min) {
                    return BuiltinToolResult.fail("max 不能小于 min");
                }
                long v = min + (long) (RANDOM.nextDouble() * (max - min + 1));
                return BuiltinToolResult.ok(String.valueOf(v));
        }
    }

    private static BuiltinToolResult password(int length, boolean up, boolean low, boolean digit, boolean symbol) {
        if (length < 4 || length > 128) {
            return BuiltinToolResult.fail("密码长度需在 4-128 之间");
        }
        if (!up && !low && !digit && !symbol) {
            up = low = digit = true;
        }
        String pools = (up ? "ABCDEFGHJKLMNPQRSTUVWXYZ" : "") + (low ? "abcdefghijkmnpqrstuvwxyz" : "")
                + (digit ? "23456789" : "") + (symbol ? "!@#$%^&*()-_=+" : "");
        List<Character> chars = new ArrayList<Character>();
        // 每个选中类别先保证至少 1 位
        if (up) {
            chars.add("ABCDEFGHJKLMNPQRSTUVWXYZ".charAt(RANDOM.nextInt(23)));
        }
        if (low) {
            chars.add("abcdefghijkmnpqrstuvwxyz".charAt(RANDOM.nextInt(23)));
        }
        if (digit) {
            chars.add("23456789".charAt(RANDOM.nextInt(8)));
        }
        if (symbol) {
            chars.add("!@#$%^&*()-_=+".charAt(RANDOM.nextInt(14)));
        }
        while (chars.size() < length) {
            chars.add(pools.charAt(RANDOM.nextInt(pools.length())));
        }
        java.util.Collections.shuffle(chars, RANDOM);
        StringBuilder sb = new StringBuilder();
        for (char c : chars) {
            sb.append(c);
        }
        return BuiltinToolResult.ok(sb.toString());
    }

    /** loan_calc：房贷月供（等额本息/等额本金） */
    public static BuiltinToolResult loanCalc(Map<String, Object> params) {
        double amount = dblOf(params.get("amount"), 0);
        double annualRate = dblOf(params.get("annual_rate"), 0);
        int years = intOf(params.get("years"), 30);
        String method = str(params.get("method"));
        if (method.isEmpty()) {
            method = "average_capital_plus_interest";
        }
        if (amount <= 0 || annualRate <= 0 || years <= 0) {
            return BuiltinToolResult.fail("参数需要 amount(贷款总额元)/annual_rate(年利率%)/years(年限)，如 1000000/3.6/30");
        }
        int n = years * 12;
        double r = annualRate / 100 / 12;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("贷款 %.0f 元，年利率 %.2f%%，%d 年（%d 期）%n", amount, annualRate, years, n));
        if ("average_capital".equals(method)) {
            double monthlyPrincipal = amount / n;
            double firstMonth = monthlyPrincipal + amount * r;
            double lastMonth = monthlyPrincipal + monthlyPrincipal * r;
            double totalInterest = amount * r * (n + 1) / 2;
            sb.append(String.format("等额本金：首月 %.2f 元，每月递减 %.2f 元，末月 %.2f 元%n",
                    firstMonth, monthlyPrincipal * r, lastMonth));
            sb.append(String.format("总利息 %.2f 元，还款总额 %.2f 元", totalInterest, amount + totalInterest));
        } else {
            double pow = Math.pow(1 + r, n);
            double monthly = amount * r * pow / (pow - 1);
            double totalInterest = monthly * n - amount;
            sb.append(String.format("等额本息：每月还款 %.2f 元（固定）%n", monthly));
            sb.append(String.format("总利息 %.2f 元，还款总额 %.2f 元", totalInterest, amount + totalInterest));
        }
        return BuiltinToolResult.ok(sb.toString());
    }

    /** color_convert：hex/rgb/hsl 互转 */
    public static BuiltinToolResult colorConvert(Map<String, Object> params) {
        String input = str(params.get("color"));
        if (input.isEmpty()) {
            return BuiltinToolResult.fail("color 不能为空，如 #5a54e8 / rgb(90,84,232) / hsl(243,77%,62%)");
        }
        int[] rgb = parseColor(input);
        if (rgb == null) {
            return BuiltinToolResult.fail("无法解析颜色: " + input + "（支持 #RGB/#RRGGBB、rgb(r,g,b)、hsl(h,s%,l%)）");
        }
        double[] hsl = rgbToHsl(rgb);
        return BuiltinToolResult.ok(String.format("hex: #%02X%02X%02X%nrgb: rgb(%d, %d, %d)%nhsl: hsl(%.0f, %.0f%%, %.0f%%)",
                rgb[0], rgb[1], rgb[2], rgb[0], rgb[1], rgb[2], hsl[0], hsl[1], hsl[2]));
    }

    private static int[] parseColor(String s) {
        if (s.startsWith("#")) {
            String hex = s.substring(1);
            if (hex.length() == 3) {
                hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
            }
            if (hex.length() != 6) {
                return null;
            }
            try {
                int v = Integer.parseInt(hex, 16);
                return new int[]{(v >> 16) & 0xFF, (v >> 8) & 0xFF, v & 0xFF};
            } catch (NumberFormatException e) {
                return null;
            }
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(rgb|hsl)\\(\\s*([\\d.]+)\\s*,\\s*([\\d.]+)%?\\s*,\\s*([\\d.]+)%?\\s*\\)").matcher(s);
        if (!m.matches()) {
            return null;
        }
        double a = Double.parseDouble(m.group(2));
        double b = Double.parseDouble(m.group(3));
        double c = Double.parseDouble(m.group(4));
        if ("rgb".equals(m.group(1))) {
            return new int[]{clamp255(a), clamp255(b), clamp255(c)};
        }
        return hslToRgb(a, b / 100, c / 100);
    }

    private static int clamp255(double v) {
        return Math.max(0, Math.min(255, (int) Math.round(v)));
    }

    private static double[] rgbToHsl(int[] rgb) {
        double r = rgb[0] / 255.0, g = rgb[1] / 255.0, b = rgb[2] / 255.0;
        double max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        double h, s, l = (max + min) / 2;
        if (max == min) {
            h = s = 0;
        } else {
            double d = max - min;
            s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
            if (max == r) {
                h = (g - b) / d + (g < b ? 6 : 0);
            } else if (max == g) {
                h = (b - r) / d + 2;
            } else {
                h = (r - g) / d + 4;
            }
            h /= 6;
        }
        return new double[]{h * 360, s * 100, l * 100};
    }

    private static int[] hslToRgb(double h, double s, double l) {
        h = (h % 360 + 360) % 360 / 360;
        double q = l < 0.5 ? l * (1 + s) : l + s - l * s;
        double p = 2 * l - q;
        return new int[]{clamp255(hueToRgb(p, q, h + 1 / 3.0) * 255),
                clamp255(hueToRgb(p, q, h) * 255), clamp255(hueToRgb(p, q, h - 1 / 3.0) * 255)};
    }

    private static double hueToRgb(double p, double q, double t) {
        if (t < 0) {
            t += 1;
        }
        if (t > 1) {
            t -= 1;
        }
        if (t < 1 / 6.0) {
            return p + (q - p) * 6 * t;
        }
        if (t < 0.5) {
            return q;
        }
        if (t < 2 / 3.0) {
            return p + (q - p) * (2 / 3.0 - t) * 6;
        }
        return p;
    }

    // ==================== 时间类 ====================

    /** current_time：当前时间（可指定时区） */
    public static BuiltinToolResult currentTime(Map<String, Object> params) {
        String tz = str(params.get("timezone"));
        if (tz.isEmpty()) {
            tz = "Asia/Shanghai";
        }
        ZonedDateTime now;
        try {
            now = ZonedDateTime.now(ZoneId.of(tz));
        } catch (Exception e) {
            return BuiltinToolResult.fail("无效时区: " + tz + "（如 Asia/Shanghai、UTC、America/New_York）");
        }
        return BuiltinToolResult.ok(String.format(
                "%s（%s）%nUnix 时间戳: %d 秒 / %d 毫秒",
                now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                WEEK_CN[now.getDayOfWeek().getValue() - 1],
                now.toEpochSecond(), now.toInstant().toEpochMilli()));
    }

    /**
     * date_calculator：日期推算。date2 给定时算两日期差；否则按 days/workdays 偏移；
     * 均未给出时返回该日期的星期。
     */
    public static BuiltinToolResult dateCalculator(Map<String, Object> params) {
        String d1 = str(params.get("date"));
        LocalDate base;
        try {
            base = d1.isEmpty() ? LocalDate.now() : LocalDate.parse(d1);
        } catch (DateTimeParseException e) {
            return BuiltinToolResult.fail("日期格式应为 yyyy-MM-dd: " + d1);
        }
        String d2 = str(params.get("date2"));
        if (!d2.isEmpty()) {
            LocalDate other;
            try {
                other = LocalDate.parse(d2);
            } catch (DateTimeParseException e) {
                return BuiltinToolResult.fail("date2 格式应为 yyyy-MM-dd: " + d2);
            }
            long diff = Math.abs(ChronoUnit.DAYS.between(base, other));
            return BuiltinToolResult.ok(String.format("%s 与 %s 相差 %d 天", base, other, diff));
        }
        int days = intOf(params.get("days"), 0);
        int workdays = intOf(params.get("workdays"), 0);
        LocalDate result = base;
        String op = "";
        if (days != 0) {
            result = result.plusDays(days);
            op = (days > 0 ? " + " : " - ") + Math.abs(days) + " 天";
        } else if (workdays != 0) {
            int remain = Math.abs(workdays);
            int step = workdays > 0 ? 1 : -1;
            while (remain > 0) {
                result = result.plusDays(step);
                if (result.getDayOfWeek() != DayOfWeek.SATURDAY && result.getDayOfWeek() != DayOfWeek.SUNDAY) {
                    remain--;
                }
            }
            op = (workdays > 0 ? " + " : " - ") + Math.abs(workdays) + " 个工作日";
        }
        return BuiltinToolResult.ok(String.format("%s%s = %s（%s）", base, op, result,
                WEEK_CN[result.getDayOfWeek().getValue() - 1]));
    }

    /** timestamp_convert：Unix 时间戳 ↔ 日期字符串 */
    public static BuiltinToolResult timestampConvert(Map<String, Object> params) {
        String tz = str(params.get("timezone"));
        ZoneId zone;
        try {
            zone = tz.isEmpty() ? ZoneId.of("Asia/Shanghai") : ZoneId.of(tz);
        } catch (Exception e) {
            return BuiltinToolResult.fail("无效时区: " + tz);
        }
        String date = str(params.get("date"));
        String ts = str(params.get("timestamp"));
        DateTimeFormatter out = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (!ts.isEmpty()) {
            try {
                long v = Long.parseLong(ts);
                // 按量级自动识别秒/毫秒：1e11 秒约在公元 5138 年，之前毫秒时间戳已出现
                LocalDateTime dt = Math.abs(v) >= 100_000_000_000L
                        ? LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(v), zone)
                        : LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(v), zone);
                return BuiltinToolResult.ok(ts + " → " + dt.format(out) + "（" + WEEK_CN[dt.getDayOfWeek().getValue() - 1] + "，" + zone.getId() + "）");
            } catch (NumberFormatException e) {
                return BuiltinToolResult.fail("timestamp 应为纯数字: " + ts);
            }
        }
        if (!date.isEmpty()) {
            LocalDateTime dt;
            try {
                // ISO 串（2026-09-05T12:00:00）与空格串（2026-09-05 12:00:00）都接受
                dt = date.length() > 10
                        ? LocalDateTime.parse(date.replace('T', ' '), out)
                        : LocalDate.parse(date).atStartOfDay();
            } catch (DateTimeParseException e) {
                return BuiltinToolResult.fail("日期格式应为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss: " + date);
            }
            long sec = dt.atZone(zone).toEpochSecond();
            return BuiltinToolResult.ok(date + " → " + sec + "（秒）/ " + sec * 1000 + "（毫秒）");
        }
        return BuiltinToolResult.fail("需要 timestamp（转日期）或 date（转时间戳）参数");
    }

    /** cron_next：cron 表达式解析 + 未来 N 次执行时间（Spring CronExpression，6 域） */
    public static BuiltinToolResult cronNext(Map<String, Object> params) {
        String expr = str(params.get("expression"));
        if (expr.isEmpty()) {
            return BuiltinToolResult.fail("expression 不能为空，如 0 0 9 * * MON-FRI");
        }
        CronExpression cron;
        try {
            cron = CronExpression.parse(expr);
        } catch (IllegalArgumentException e) {
            return BuiltinToolResult.fail("非法 cron 表达式: " + e.getMessage() + "（Spring 6 域格式：秒 分 时 日 月 周）");
        }
        int count = Math.min(Math.max(intOf(params.get("count"), 3), 1), 10);
        StringBuilder sb = new StringBuilder("cron [" + expr + "] 未来 " + count + " 次执行时间：\n");
        LocalDateTime next = LocalDateTime.now();
        for (int i = 0; i < count; i++) {
            next = cron.next(next);
            if (next == null) {
                sb.append("（后续无更多执行时间）");
                break;
            }
            sb.append(next.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .append("（").append(WEEK_CN[next.getDayOfWeek().getValue() - 1]).append("）\n");
        }
        return BuiltinToolResult.ok(sb.toString().trim());
    }

    /** 数字格式化：整数值去掉小数点，否则保留最多 6 位有效小数 */
    private static String formatNum(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e15) {
            return String.valueOf((long) v);
        }
        return String.valueOf(Math.round(v * 1e6) / 1e6);
    }
}
