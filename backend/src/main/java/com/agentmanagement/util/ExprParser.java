package com.agentmanagement.util;

import java.util.Map;

/**
 * 数学表达式求值器（calculator 内置工具专用）：递归下降解析，白名单词法，
 * 不使用 ScriptEngine（防脚本注入）。支持 + - * / % ^ 括号、一元负号、
 * 函数 sqrt/sin/cos/tan/abs/round/floor/ceil/log/ln/pow/min/max 与常量 pi/e。
 * 纯静态工具类，无状态。
 */
public final class ExprParser {

    /** 函数名 → 实现（一元函数）；pow/min/max 为二元，单独处理 */
    private interface Func1 {
        double apply(double x);
    }

    private static final Map<String, Func1> FUNCS = new java.util.HashMap<String, Func1>();

    static {
        FUNCS.put("sqrt", new Func1() { public double apply(double x) { return Math.sqrt(x); } });
        FUNCS.put("sin", new Func1() { public double apply(double x) { return Math.sin(x); } });
        FUNCS.put("cos", new Func1() { public double apply(double x) { return Math.cos(x); } });
        FUNCS.put("tan", new Func1() { public double apply(double x) { return Math.tan(x); } });
        FUNCS.put("abs", new Func1() { public double apply(double x) { return Math.abs(x); } });
        FUNCS.put("round", new Func1() { public double apply(double x) { return Math.round(x); } });
        FUNCS.put("floor", new Func1() { public double apply(double x) { return Math.floor(x); } });
        FUNCS.put("ceil", new Func1() { public double apply(double x) { return Math.ceil(x); } });
        FUNCS.put("log", new Func1() { public double apply(double x) { return Math.log10(x); } });
        FUNCS.put("ln", new Func1() { public double apply(double x) { return Math.log(x); } });
    }

    private final String src;
    private int pos;

    private ExprParser(String src) {
        this.src = src;
        this.pos = 0;
    }

    /** 求值入口；语法/词法错误抛 IllegalArgumentException，由调用方转 fail */
    public static double eval(String expression) {
        ExprParser p = new ExprParser(expression);
        double v = p.parseExpr();
        p.skipWs();
        if (p.pos < p.src.length()) {
            throw new IllegalArgumentException("无法解析的字符: '" + p.src.charAt(p.pos) + "'（位置 " + (p.pos + 1) + "）");
        }
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            throw new IllegalArgumentException("结果不是有限数（除零或超出范围）");
        }
        return v;
    }

    /** 加减层 */
    private double parseExpr() {
        double v = parseTerm();
        while (true) {
            skipWs();
            if (match('+')) {
                v += parseTerm();
            } else if (match('-')) {
                v -= parseTerm();
            } else {
                return v;
            }
        }
    }

    /** 乘除模层 */
    private double parseTerm() {
        double v = parseUnary();
        while (true) {
            skipWs();
            if (match('*')) {
                v *= parseUnary();
            } else if (match('/')) {
                v /= parseUnary();
            } else if (match('%')) {
                v %= parseUnary();
            } else {
                return v;
            }
        }
    }

    /** 一元层：负号 / 幂（右结合，-2^2 = -4） */
    private double parseUnary() {
        skipWs();
        if (match('-')) {
            return -parseUnary();
        }
        if (match('+')) {
            return parseUnary();
        }
        return parsePower();
    }

    private double parsePower() {
        double base = parseAtom();
        skipWs();
        if (match('^')) {
            return Math.pow(base, parseUnary());
        }
        return base;
    }

    /** 原子：数字 / 常量 / 函数 / 括号 */
    private double parseAtom() {
        skipWs();
        if (pos >= src.length()) {
            throw new IllegalArgumentException("表达式意外结束");
        }
        char c = src.charAt(pos);
        if (c == '(') {
            pos++;
            double v = parseExpr();
            skipWs();
            if (!match(')')) {
                throw new IllegalArgumentException("缺少右括号");
            }
            return v;
        }
        if (Character.isDigit(c) || c == '.') {
            return parseNumber();
        }
        if (Character.isLetter(c) || c == '_') {
            return parseIdent();
        }
        throw new IllegalArgumentException("无法解析的字符: '" + c + "'（位置 " + (pos + 1) + "）");
    }

    private double parseNumber() {
        int start = pos;
        while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) {
            pos++;
        }
        try {
            return Double.parseDouble(src.substring(start, pos));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("非法数字: " + src.substring(start, pos));
        }
    }

    private double parseIdent() {
        int start = pos;
        while (pos < src.length() && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_')) {
            pos++;
        }
        String name = src.substring(start, pos).toLowerCase();
        if ("pi".equals(name)) {
            return Math.PI;
        }
        if ("e".equals(name)) {
            return Math.E;
        }
        Func1 f = FUNCS.get(name);
        if (f != null) {
            skipWs();
            if (!match('(')) {
                throw new IllegalArgumentException("函数 " + name + " 后缺少 (");
            }
            double arg = parseExpr();
            // round 支持可选第二参数指定小数位数：round(3.14159, 2) = 3.14
            if ("round".equals(name)) {
                skipWs();
                if (match(',')) {
                    double digits = parseExpr();
                    skipWs();
                    if (!match(')')) {
                        throw new IllegalArgumentException("函数 round 缺少右括号");
                    }
                    double scale = Math.pow(10, digits);
                    return Math.round(arg * scale) / scale;
                }
            }
            skipWs();
            if (!match(')')) {
                throw new IllegalArgumentException("函数 " + name + " 缺少右括号");
            }
            return f.apply(arg);
        }
        if ("pow".equals(name) || "min".equals(name) || "max".equals(name)) {
            skipWs();
            if (!match('(')) {
                throw new IllegalArgumentException("函数 " + name + " 后缺少 (");
            }
            double a = parseExpr();
            skipWs();
            if (!match(',')) {
                throw new IllegalArgumentException("函数 " + name + " 需要两个参数（逗号分隔）");
            }
            double b = parseExpr();
            skipWs();
            if (!match(')')) {
                throw new IllegalArgumentException("函数 " + name + " 缺少右括号");
            }
            if ("pow".equals(name)) {
                return Math.pow(a, b);
            }
            return "min".equals(name) ? Math.min(a, b) : Math.max(a, b);
        }
        throw new IllegalArgumentException("未知标识符: " + name + "（可用函数 sqrt/sin/cos/tan/abs/round/floor/ceil/log/ln/pow/min/max，常量 pi/e）");
    }

    private void skipWs() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
            pos++;
        }
    }

    private boolean match(char c) {
        if (pos < src.length() && src.charAt(pos) == c) {
            pos++;
            return true;
        }
        return false;
    }
}
