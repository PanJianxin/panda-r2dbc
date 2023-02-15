package com.jxpanda.r2dbc.spring.data.core.enhance.query.criteria;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存lambda 和 字段名
 * package访问修饰，禁止外部修改
 */
class AccessorCache {

    private static final Map<AccessorFunction<?, ?>, String> CACHE = new HashMap<>();


    static boolean containsKey(AccessorFunction<?, ?> key) {
        return CACHE.containsKey(key);
    }

    static void put(AccessorFunction<?, ?> key, String value) {
        CACHE.put(key, value);
    }


    static String get(AccessorFunction<?, ?> key) {
        return CACHE.get(key);
    }


}
