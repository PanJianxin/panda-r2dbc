package com.jxpanda.r2dbc.spring.data.infrastructure.kit;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.criteria.AccessorFunction;
import com.jxpanda.r2dbc.spring.data.infrastructure.constant.StringConstant;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.GenericTypeResolver;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 反射工具类，提供反射相关的快捷操作
 *
 * @author Panda
 * @since 2020-10-24
 */
@Slf4j
@UtilityClass
public final class ReflectionKit {

    private static final String GET_PREFIX = "get";
    private static final String GET_CLASS = "getClass";
    private static final String SET_PREFIX = "set";
    private static final String IS_PREFIX = "is";
    /**
     * writeReplace函数的名字
     * 反射使用这个函数可以解析出Lambda表达式对应的字段
     */
    private static final String WRITE_REPLACE = "writeReplace";

    /**
     * 默认值列表
     */
    private static final Map<Class<?>, Object> DEFAULT_VALUE_MAP;

    static {
        DEFAULT_VALUE_MAP = new HashMap<>();
        DEFAULT_VALUE_MAP.put(Collection.class, Collections.emptyList());
        DEFAULT_VALUE_MAP.put(List.class, Collections.emptyList());
        DEFAULT_VALUE_MAP.put(Set.class, Collections.emptyList());
        DEFAULT_VALUE_MAP.put(Map.class, Collections.emptyMap());
        DEFAULT_VALUE_MAP.put(Object.class, new Object());
        DEFAULT_VALUE_MAP.put(Byte.class, 0);
        DEFAULT_VALUE_MAP.put(Short.class, 0);
        DEFAULT_VALUE_MAP.put(Integer.class, 0);
        DEFAULT_VALUE_MAP.put(Long.class, 0L);
        DEFAULT_VALUE_MAP.put(Float.class, 0.0F);
        DEFAULT_VALUE_MAP.put(Double.class, 0.0);
        DEFAULT_VALUE_MAP.put(BigDecimal.class, BigDecimal.ZERO);
        DEFAULT_VALUE_MAP.put(Boolean.class, false);
        DEFAULT_VALUE_MAP.put(String.class, StringConstant.BLANK);
    }


    /**
     * 获取字段类型
     *
     * @param clazz     实体类型
     * @param fieldName 字段名称
     * @return 字段的类型
     */
    public static Class<?> getFieldType(Class<?> clazz, String fieldName) {
        Field field = getFieldMap(clazz).get(fieldName);
        return field == null ? null : field.getType();
    }

    /**
     * 获取字段值
     *
     * @param entity    实体
     * @param fieldName 字段名称
     * @return 字段值
     */
    public static Object getFieldValue(Object entity, String fieldName) {
        Method getter = getGetterMap(entity.getClass()).get(fieldName);
        return invokeGetter(getter, entity);
    }

    /**
     * 获取字段值
     *
     * @param entity 实体
     * @param field  字段
     * @return 字段值
     */
    public static Object getFieldValue(Object entity, Field field) {
        Method getter = getGetterMap(entity.getClass()).get(field.getName());
        return invokeGetter(getter, entity);
    }

    /**
     * 执行getter函数
     *
     * @param object 目标对象
     * @return 返回值
     */
    public static Object invokeGetter(Method getter, Object object) {
        try {
            return getter.invoke(object);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("[INVOKE GETTER ERROR]", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 设置字段的值
     *
     * @param entity    实体
     * @param fieldName 字段（字符串结构）
     */
    public static void setFieldValue(Object entity, String fieldName, Object value) {
        Method setter = getSetterMap(entity.getClass()).get(fieldName);
        invokeSetter(setter, entity, value);
    }

    /**
     * 设置字段的值
     *
     * @param entity 实体
     * @param field  字段
     */
    public static void setFieldValue(Object entity, Field field, Object value) {
        Method setter = getSetterMap(entity.getClass()).get(field.getName());
        invokeSetter(setter, entity, value);
    }

    /**
     * 执行setter函数
     *
     * @param setter setter函数
     * @param object 目标对象
     * @param value  值
     */
    public static void invokeSetter(Method setter, Object object, Object value) {
        try {
            setter.invoke(object, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("[INVOKE SETTER ERROR]", e);
            throw new RuntimeException(e);
        }
    }


    /**
     * 初始化字段
     *
     * @param entity 实体
     * @param field  字段
     */
    public static void initField(Object entity, Field field) {
        Class<?> fieldType = field.getType();
        Object defaultValue = DEFAULT_VALUE_MAP.get(fieldType);
        if (defaultValue == null) {
            if (fieldType.isEnum()) {
                // noinspection rawtypes,unchecked
                defaultValue = Enum.valueOf((Class) fieldType, StringConstant.UNKNOWN);
            } else {
                Constructor<?>[] constructors = fieldType.getConstructors();
                if (constructors.length > 0) {
                    defaultValue = newInstance(fieldType);
                }
            }
        }
        setFieldValue(entity, field, defaultValue);
    }

    /**
     * 使用空构造器新建对象
     *
     * @param clazz 目标类
     * @return 对象实例
     */
    public static <T> T newInstance(Class<T> clazz) {
        try {
            return clazz.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>
     * 获取该类的所有字段Map
     * key是字段名
     * value是字段对象
     * </p>
     *
     * @param clazz 目标类
     * @return fieldMap
     */
    public static Map<String, Field> getFieldMap(Class<?> clazz) {
        return getFieldList(clazz).stream().collect(Collectors.toMap(Field::getName, Function.identity()));
    }

    /**
     * <p>
     * 获取该类的所有字段列表
     * </p>
     *
     * @param clazz 目标类
     * @return 字段列表
     */
    public static List<Field> getFieldList(Class<?> clazz) {

        Field[] fields = clazz.getDeclaredFields();

        Field[] superFields = {};

        if (clazz.getSuperclass() != null) {
            superFields = clazz.getSuperclass().getDeclaredFields();
        }

        return CollectionKit.distinctMerge(Field::getName, fields, superFields).stream()
                /* 过滤静态字段 */
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                /* 过滤 transient关键字修饰的字段 */
                .filter(f -> !Modifier.isTransient(f.getModifiers()))
                .toList();

    }

    /**
     * <p>
     * 获取该类的所有getter函数的Map
     * key是字段名
     * value是getter函数
     * </p>
     *
     * @param clazz 目标类
     * @return getterMap
     */
    public static Map<String, Method> getGetterMap(Class<?> clazz) {
        Method[] methods = clazz.getMethods();
        Map<String, Field> fieldMap = getFieldMap(clazz);
        Map<String, Method> result = new HashMap<>(fieldMap.size());
        for (Method method : methods) {
            String methodName = method.getName();
            // 判断是否是getter函数，满足以下条件则判定为是一个getter函数
            // 非静态函数
            boolean isGetter = !Modifier.isStatic(method.getModifiers())
                               // 排除getClass这个函数
                               && !methodName.equals(GET_CLASS)
                               // 以get或者is做前缀的函数
                               && (methodName.startsWith(GET_PREFIX) || methodName.startsWith(IS_PREFIX));
            if (isGetter) {
                String fieldName = StringKit.uncapitalize(method.getName()
                        .replaceFirst("^" + GET_PREFIX, "")
                        .replaceFirst("^" + IS_PREFIX, ""));
                if (fieldMap.containsKey(fieldName)) {
                    result.put(fieldName, method);
                }
            }
        }
        return result;
    }

    /**
     * <p>
     * 获取该类的所有setter函数的Map
     * key是字段名
     * value是setter函数
     * </p>
     *
     * @param clazz 反射类
     * @return setterMap
     */
    public static Map<String, Method> getSetterMap(Class<?> clazz) {
        Method[] methods = clazz.getMethods();
        Map<String, Field> fieldMap = getFieldMap(clazz);
        Map<String, Method> result = new HashMap<>(fieldMap.size());
        for (Method method : methods) {
            if (!Modifier.isStatic(method.getModifiers()) && method.getName().startsWith(SET_PREFIX)) {
                String fieldName = StringKit.uncapitalize(method.getName().replaceFirst(SET_PREFIX, ""));
                if (fieldMap.containsKey(fieldName)) {
                    result.put(fieldName, method);
                }
            }
        }
        return result;
    }


    /**
     * 使用getter和setter复制
     *
     * @param source 原对象
     * @param target 目标对象
     * @return 目标对象
     */
    public static Object copy(Object source, Object target) {
        Map<String, Method> getterMap = getGetterMap(source.getClass());
        Map<String, Method> setterMap = getSetterMap(target.getClass());
        getterMap.forEach((k, v) -> {
            Method getterMethod = getterMap.get(k);
            Method setterMethod = setterMap.get(k);
            if (getterMethod != null && setterMethod != null) {
                Object value = invokeGetter(getterMethod, source);
                Class<?> parameterType = setterMethod.getParameterTypes()[0];
                if (value != null && parameterType.isAssignableFrom(value.getClass())) {
                    invokeSetter(setterMethod, target, value);
                }
            }
        });
        return target;
    }

    /**
     * 获取泛型的类型
     *
     * @param clazz       目标类
     * @param genericType 从中解析类型参数的泛型接口或超类，一般是目标类所继承的超类或者所实现的接口
     * @param index       下标
     * @return 泛型的类型
     */
    public static <T> Class<T> getSuperClassGenericType(final Class<?> clazz, final Class<?> genericType, int index) {
        Class<?>[] classes = GenericTypeResolver.resolveTypeArguments(clazz, genericType);
        return classes == null ? null : cast(classes[index]);
    }

    /**
     * 获取lambda表达式对应的字段
     *
     * @param accessorFunction 访问函数（Lambda表达式）
     * @return field字段
     */
    public static <T, R> Field getFieldFromAccessorFunction(AccessorFunction<T, R> accessorFunction) {

        // 从function取出序列化方法
        Method writeReplaceMethod;
        try {
            writeReplaceMethod = accessorFunction.getClass().getDeclaredMethod(WRITE_REPLACE);
        } catch (NoSuchMethodException e) {
            log.error("[REFLECTION ERROR] writeReplace function not found");
            throw new RuntimeException(e);
        }

        // 从序列化方法取出序列化的lambda信息
        boolean isAccessible = writeReplaceMethod.canAccess(accessorFunction);
        writeReplaceMethod.setAccessible(true);
        SerializedLambda serializedLambda;
        try {
            serializedLambda = (SerializedLambda) writeReplaceMethod.invoke(accessorFunction);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("[REFLECTION ERROR] writeReplaceMethod function invoke error");
            throw new RuntimeException(e);
        }
        writeReplaceMethod.setAccessible(isAccessible);

        // 从lambda信息取出method、field、class等
        String implMethodName = serializedLambda.getImplMethodName();
        // 确保方法是符合规范的get方法，boolean类型是is开头
        if (!implMethodName.startsWith(IS_PREFIX) && !implMethodName.startsWith(GET_PREFIX)) {
            throw new RuntimeException("Function: " + implMethodName + ",does not conform to the java bean specification");
        }

        // get方法开头为 is 或者 get，将方法名 去除is或者get，然后首字母小写，就是字段名
        int prefixLen = implMethodName.startsWith(IS_PREFIX) ? IS_PREFIX.length() : GET_PREFIX.length();

        String fieldName = StringKit.uncapitalize(implMethodName.substring(prefixLen));

        Field field;
        try {
            field = Class.forName(serializedLambda.getImplClass().replace(StringConstant.SLASH, StringConstant.DOT)).getDeclaredField(fieldName);
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            log.error("[REFLECTION ERROR] get field error");
            throw new RuntimeException(e);
        }

        return field;
    }

    @SuppressWarnings("unchecked")
    public static <T> T cast(Object object) {
        return (T) object;
    }

}
