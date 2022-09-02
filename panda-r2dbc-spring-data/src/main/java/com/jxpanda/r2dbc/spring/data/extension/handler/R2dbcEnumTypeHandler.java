package com.jxpanda.r2dbc.spring.data.extension.handler;

import com.jxpanda.r2dbc.spring.data.extension.StandardEnum;
import com.jxpanda.r2dbc.spring.data.extension.annotation.EnumValue;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 枚举处理器
 */
public enum R2dbcEnumTypeHandler implements R2dbcTypeHandler<Enum<?>, Object> {

    INSTANCE;

    private static final Map<Class<? extends Enum<?>>, Map<String, Enum<?>>> ENUM_CACHE = new HashMap<>();

    @Override
    public Writer<Object, Enum<?>> getWriter(RelationalPersistentProperty property) {
        return (enumConstant) -> {

            // 如果实现了StandardEnum接口，直接返回code
            if (enumConstant instanceof StandardEnum enumObject) {
                return enumObject.getCode();
            }

            // 否则找@EnumValue注解标识的字段，返回该字段的值
            Field field = Arrays.stream(property.getType().getDeclaredFields()).filter(it -> it.isAnnotationPresent(EnumValue.class))
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
    public Reader<Enum<?>, Object> getReader(RelationalPersistentProperty property) {
        return (value) -> {
            Map<String, Enum<?>> enumMap = getEnumMap(property);
            return enumMap.getOrDefault(value.toString(), enumMap.get("0"));
        };
    }


    @SuppressWarnings("unchecked")
    private Map<String, Enum<?>> getEnumMap(RelationalPersistentProperty property) {
        Class<? extends Enum<?>> objectClass = (Class<? extends Enum<?>>) property.getType();
        return ENUM_CACHE.computeIfAbsent(objectClass, (k) -> Arrays.stream(objectClass.getEnumConstants())
                .collect(Collectors.toMap(it -> write(it, property).toString(), it -> it)));
    }

}
