package com.agentmanagement.service.builtin.handler;

/**
 * 内置工具 handler 共用的参数读取工具（与 BuiltinToolServiceImpl 内私有辅助同语义）。
 * 容错 Number/字符串/空值，取不到回默认。
 */
public final class Params {

    private Params() {
    }

    public static String str(Object o) {
        return o != null ? o.toString().trim() : "";
    }

    public static int intOf(Object o, int def) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            return o != null ? Integer.parseInt(o.toString().trim()) : def;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static long longOf(Object o, long def) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            return o != null ? Long.parseLong(o.toString().trim()) : def;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static double dblOf(Object o, double def) {
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        try {
            return o != null ? Double.parseDouble(o.toString().trim()) : def;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static boolean boolOf(Object o) {
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        return o != null && ("true".equalsIgnoreCase(o.toString()) || "1".equals(o.toString()));
    }

    public static String abbreviate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
