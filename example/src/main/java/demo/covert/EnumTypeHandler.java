package demo.covert;

import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.jxpanda.r2dbc.spring.data.mapping.annotation.EnumValue;
import demo.model.Order;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class EnumTypeHandler {

    private static final Map<Class<?>, Map<String, Enum<?>>> ENUM_CACHE = new HashMap<>();

    public static Enum<?> translate(Class<?> enumClass, String value) {
        Map<String, Enum<?>> enumMap = ENUM_CACHE.computeIfAbsent(enumClass, (k) -> Arrays.stream(enumClass.getEnumConstants())
                .collect(Collectors.toMap(it -> {
                    Field valueField = Arrays.stream(enumClass.getDeclaredFields()).filter(field -> field.isAnnotationPresent(EnumValue.class))
                            .findFirst().orElse(null);
                    if (valueField != null){
                        return ReflectionKit.getFieldValue(it, valueField.getName()).toString();
                    }
                   return "UNKNOWN";
                }, it -> (Enum<?>) it)));
        return enumMap.getOrDefault(value, enumMap.get("0"));
    }

    public static void main(String[] args) {
        System.out.println(translate(Order.Device.class, "1"));
        System.out.println(translate(Order.Device.class, "2"));
        System.out.println(translate(Order.Status.class, "1"));
        System.out.println(translate(Order.Status.class, "7"));
    }

}
