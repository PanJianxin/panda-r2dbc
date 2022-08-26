package com.jxpanda.r2dbc.spring.data.mapping.handler;

import com.jxpanda.r2dbc.spring.data.mapping.annotation.EnumValue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 枚举处理器
 */
public final class EnumTypeHandler extends TypeHandler<Enum<?>, Object> {

    private static final Map<Class<? extends Enum<?>>, Map<Object, Enum<?>>> ENUM_CACHE = new HashMap<>();


    @Override
    public Serialize<Enum<?>, Object> getSerializer(Class<? extends Enum<?>> objectClass) {
        return (enumConstant) -> {

            // 如果实现了StandardEnum接口，直接返回code
            if (enumConstant instanceof StandardEnum enumObject) {
                return enumObject.getCode();
            }

            // 否则找@EnumValue注解标识的字段，返回该字段的值
            Field field = Arrays.stream(objectClass.getDeclaredFields()).filter(it -> it.isAnnotationPresent(EnumValue.class))
                    .findFirst().orElse(null);

            if (field != null) {
                field.setAccessible(true);
                try {
                    return field.get(enumConstant);
                } catch (IllegalAccessException ignored) {

                }
            }

            // 容灾：默认返回枚举的序号
            return enumConstant.ordinal();
        };
    }

    @Override
    public Deserializer<Enum<?>, Object> getDeserializer(Class<? extends Enum<?>> objectClass) {
        return (value) -> {
            Map<Object, Enum<?>> enumMap = getEnumMap(objectClass);
            return enumMap.getOrDefault(value, enumMap.get(0));
        };
    }


    private Map<Object, Enum<?>> getEnumMap(Class<? extends Enum<?>> objectClass) {
        return ENUM_CACHE.computeIfAbsent(objectClass, (k) -> Arrays.stream(objectClass.getEnumConstants())
                .collect(Collectors.toMap(it -> serialize(objectClass, it), it -> it)));
    }

}
