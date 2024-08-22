package com.jxpanda.r2dbc.spring.data.core.enhance.handler;

import com.jxpanda.r2dbc.spring.data.core.enhance.StandardEnum;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.EnumValue;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.ReflectionKit;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 枚举处理器
 */
public class R2dbcEnumTypeHandler implements R2dbcTypeHandler<Enum<?>, Object> {

    private final TwoKeyMap<Class<? extends Enum<?>>, String, Enum<?>> enumCache = new TwoKeyMap<>();

    @Override
    public Writer<Object, Enum<?>> getWriter(RelationalPersistentProperty property) {
        return (enumConstant) -> {

            // 如果实现了StandardEnum接口，直接返回code
            if (enumConstant instanceof StandardEnum standardEnum) {
                return standardEnum.getCode();
            }

            // 否则找@EnumValue注解标识的字段，返回该字段的值
            Field field = Arrays.stream(property.getType().getDeclaredFields())
                    .filter(it -> it.isAnnotationPresent(EnumValue.class))
                    .findFirst()
                    .orElse(null);

            if (field != null) {
                return ReflectionKit.getFieldValue(enumConstant, field);
            }

            // 容灾：默认返回枚举的序号
            return enumConstant.ordinal();
        };
    }

    @Override
    public Reader<Enum<?>, Object> getReader(RelationalPersistentProperty property) {
        return (value) -> getOrDefault(property, value.toString());
    }

    private Enum<?> getOrDefault(RelationalPersistentProperty property, String value) {
        Map<String, Enum<?>> enumMap = getEnumMap(property);
        return enumMap.getOrDefault(value, enumMap.get("0"));
    }

    private Map<String, Enum<?>> getEnumMap(RelationalPersistentProperty property) {
        Class<? extends Enum<?>> objectClass = ReflectionKit.cast(property.getType());
        return enumCache.getOrCreateLevelOneCache(objectClass, (k) -> Arrays.stream(objectClass.getEnumConstants())
                .collect(Collectors.toMap(it -> write(it, property).toString(), it -> it)));
    }

}
