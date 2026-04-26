package com.harbin.dataplatform.common.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class TypeConverter {

    private TypeConverter() {
    }

    public static Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Long l) return l;
        if (obj instanceof Number n) return n.longValue();
        return Long.parseLong(obj.toString());
    }

    public static Integer toInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer i) return i;
        if (obj instanceof Number n) return n.intValue();
        return Integer.parseInt(obj.toString());
    }

    public static Double toDouble(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.doubleValue();
        return Double.parseDouble(obj.toString());
    }

    public static String toStringOrDefault(Object obj, String defaultValue) {
        if (obj == null) return defaultValue;
        String value = obj.toString();
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public static List<Double> parseDoubleList(Object obj) {
        if (obj == null || obj.toString().isBlank()) return Collections.emptyList();
        return Arrays.stream(obj.toString().split(","))
                .map(Double::parseDouble)
                .toList();
    }

    public static List<Long> parseLongList(Object obj) {
        if (obj == null || obj.toString().isBlank()) return Collections.emptyList();
        return Arrays.stream(obj.toString().split(","))
                .mapToDouble(Double::parseDouble)
                .mapToLong(Math::round)
                .boxed()
                .toList();
    }
}
