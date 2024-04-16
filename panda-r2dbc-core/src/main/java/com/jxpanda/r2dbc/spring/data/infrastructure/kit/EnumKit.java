package com.jxpanda.r2dbc.spring.data.infrastructure.kit;


@SuppressWarnings({"unchecked", "rawtypes"})
public class EnumKit {

    private static final String ENUM_CODE = "code";

    private static final String DEFAULT_ENUM_VALUE = "UNKNOWN";

    public static int translate2Code(String enumName, Class<?> enumClass) {
        return (int) ReflectionKit.getFieldValue(Enum.valueOf((Class) enumClass, enumName), ENUM_CODE);
    }

    public static Enum getDefaultEnum(Class<?> clazz) {
        return Enum.valueOf((Class) clazz, DEFAULT_ENUM_VALUE);
    }


}
