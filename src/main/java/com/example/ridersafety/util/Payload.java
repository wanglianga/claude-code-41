package com.example.ridersafety.util;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/** 请求体（Map）取值辅助 */
public class Payload {

    public static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    public static String require(Map<String, Object> m, String key, String label) {
        String s = str(m, key);
        if (s == null) {
            throw com.example.ridersafety.config.ApiException.badRequest("缺少必填项: " + label);
        }
        return s;
    }

    public static Long lng(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        String s = v.toString().trim();
        return s.isEmpty() ? null : Long.parseLong(s);
    }

    public static Integer integer(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        String s = v.toString().trim();
        return s.isEmpty() ? null : Integer.parseInt(s);
    }

    public static Boolean bool(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(v.toString());
    }

    public static BigDecimal dec(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : new BigDecimal(s);
    }

    /** 解析 ISO 日期时间，支持 "2026-09-12T10:30" 与 "2026-09-12 10:30" */
    public static LocalDateTime dateTime(Map<String, Object> m, String key) {
        String s = str(m, key);
        if (s == null) return null;
        s = s.replace(' ', 'T');
        if (s.length() == 16) s = s + ":00";
        return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
