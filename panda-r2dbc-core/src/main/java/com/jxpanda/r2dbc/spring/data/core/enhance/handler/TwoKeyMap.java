package com.jxpanda.r2dbc.spring.data.core.enhance.handler;

import com.jxpanda.r2dbc.spring.data.infrastructure.kit.ReflectionKit;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

class TwoKeyMap<K1, K2, V> {

    private final Map<K1, Map<K2, V>> cacheMap = new HashMap<>(8);

    V get(K1 k1, K2 k2) {
        return cacheMap.get(k1).get(k2);
    }

    V getOrDefault(K1 k1, K2 k2, V defaultValue) {
        return getLevelOneCache(k1)
                .computeIfAbsent(k2, key -> defaultValue);
    }

    V getOrCreate(K1 k1, K2 k2, Function<K2, V> valueCreator) {
        return getLevelOneCache(k1)
                .computeIfAbsent(k2, valueCreator);
    }

    V getOrNewInstance(K1 k1, K2 k2, Class<? extends V> valueClass) {
        return getOrCreate(k1, k2, key -> ReflectionKit.newInstance(valueClass));
    }

    Map<K2, V> getLevelOneCache(K1 k1) {
        return getOrCreateLevelOneCache(k1, k -> new HashMap<>(8));
    }

    Map<K2, V> getOrCreateLevelOneCache(K1 k1, Function<K1, Map<K2, V>> level2Creator) {
        return cacheMap.computeIfAbsent(k1, level2Creator);
    }

    void put(K1 k1, K2 k2, V value) {
        getLevelOneCache(k1).put(k2, value);
    }


}