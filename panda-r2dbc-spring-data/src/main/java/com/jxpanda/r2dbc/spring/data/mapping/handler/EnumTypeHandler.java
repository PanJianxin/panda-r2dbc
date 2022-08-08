package com.jxpanda.r2dbc.spring.data.mapping.handler;

import com.jxpanda.r2dbc.spring.data.mapping.annotation.EnumValue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class EnumTypeHandler {

    private static final Map<Class<?>, Map<String, Enum<?>>> ENUM_CACHE = new HashMap<>();

    public static Enum<?> translate(Class<?> enumClass, String value) {
        Map<String, Enum<?>> enumMap = ENUM_CACHE.computeIfAbsent(enumClass, (k) -> Arrays.stream(enumClass.getEnumConstants())
                .collect(Collectors.toMap(it -> getEnumValue(it, enumClass), it -> (Enum<?>) it)));
        return enumMap.getOrDefault(value, enumMap.get("0"));
    }

    private static String getEnumValue(Object entity, Class<?> enumClass) {
        Field field = Arrays.stream(enumClass.getDeclaredFields()).filter(it -> it.isAnnotationPresent(EnumValue.class))
                .findFirst().orElse(null);

        if (field != null) {
            field.setAccessible(true);
            try {
                return field.get(entity).toString();
            } catch (IllegalAccessException ignored) {

            }
        }

        return "0";
    }


}
