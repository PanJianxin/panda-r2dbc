/*
 * Copyright (c) 2011-2020, baomidou (jobob@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jxpanda.r2dbc.spring.data.extension.support;


import com.jxpanda.r2dbc.spring.data.extension.constant.StringConstant;
import lombok.SneakyThrows;
import org.springframework.core.GenericTypeResolver;

import java.lang.annotation.Annotation;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 反射工具类，提供反射相关的快捷操作
 * 暂时先从mybatis-plus那边抄过来，回头再改
 *
 * @author Panda
 * @since 2020-10-24
 */
@SuppressWarnings("all")
public final class ReflectionKit {

    private static final String GET_PREFIX = "get";
    private static final String GET_CLASS = "getClass";
    private static final String SET_PREFIX = "set";
    private static final String IS_PREFIX = "is";
    private static final String WRITE_REPLACE = "writeReplace";

    private static final Map<Class<?>, Object> DEFAULT_VALUE_MAP = new HashMap<>() {{
        put(Collection.class, Collections.emptyList());
        put(List.class, Collections.emptyList());
        put(Set.class, Collections.emptyList());
        put(Map.class, Collections.emptyMap());
        put(Object.class, new Object());
        put(Byte.class, 0);
        put(Short.class, 0);
        put(Integer.class, 0);
        put(Long.class, 0L);
        put(Float.class, 0.0F);
        put(Double.class, 0.0);
        put(BigDecimal.class, BigDecimal.ZERO);
        put(Boolean.class, false);
        put(String.class, StringConstant.BLANK);
    }};

    /**
     * 获取字段类型
     *
     * @param clazz     实体类型
     * @param fieldName 字段名称
     * @return 字段的类型
     */
    @SneakyThrows
    public static Class<?> getFieldType(Class<?> clazz, String fieldName) {
        Field field = getFieldMap(clazz).get(fieldName);
        return field == null ? null : field.getType();
    }

    /**
     * 获取字段值
     *
     * @param entity    实体
     * @param fieldName 字段名称
     * @return 属性值
     */
    @SneakyThrows
    public static Object getFieldValue(Object entity, String fieldName) {
        Map<String, Field> fieldMaps = getFieldMap(entity.getClass());
        Field field = fieldMaps.get(fieldName);
        field.setAccessible(true);
        return field.get(entity);
    }

    /**
     * 获取字段值
     *
     * @param entity 实体
     * @param field  字段
     * @return 属性值
     */
    @SneakyThrows
    public static Object getFieldValue(Object entity, Field field) {
        field.setAccessible(true);
        return field.get(entity);
    }

    /**
     * 设置字段的值
     *
     * @param entity 实体
     * @param field  字段
     */
    @SneakyThrows
    public static void setFieldValue(Object entity, Field field, Object value) {
        field.setAccessible(true);
        field.set(entity, value);
    }

    /**
     * 设置字段的值
     *
     * @param entity 实体
     * @param field  字段（字符串结构）
     */
    @SneakyThrows
    public static void setFieldValue(Object entity, String field, Object value) {
        Field entityFiled = getFieldMap(entity.getClass()).get(field);
        setFieldValue(entity, entityFiled, value);
    }

    /**
     * 初始化字段
     *
     * @param entity 实体
     * @param field  字段
     */
    @SneakyThrows
    public static void initField(Object entity, Field field) {
        field.setAccessible(true);
        Class<?> fieldType = field.getType();
        Object defaultValue = DEFAULT_VALUE_MAP.get(fieldType);
        if (defaultValue == null) {
            if (fieldType.isEnum()) {
                defaultValue = Enum.valueOf((Class) fieldType, StringConstant.UNKNOWN);
            } else {
                Constructor<?>[] constructors = fieldType.getConstructors();
                if (constructors.length > 0) {
                    defaultValue = fieldType.getConstructor().newInstance();
                }
            }
        }
        field.set(entity, defaultValue);
    }

    /**
     * <p>
     * 获取该类的所有属性列表
     * </p>
     *
     * @param clazz 反射类
     */
    public static Map<String, Field> getFieldMap(Class<?> clazz) {
        return getFieldList(clazz).stream().collect(Collectors.toMap(Field::getName, Function.identity()));
    }

    /**
     * <p>
     * 获取该类的所有属性列表
     * </p>
     *
     * @param clazz 反射类
     */
    public static List<Field> getFieldList(Class<?> clazz) {

        Field[] fields = clazz.getDeclaredFields();

        Field[] superFields = {};

        if (clazz.getSuperclass() != null) {
            superFields = clazz.getSuperclass().getDeclaredFields();
        }

        return CollectionKit.distinctMerge(Field::getName, fields, superFields).stream()
                /* 过滤静态属性 */
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                /* 过滤 transient关键字修饰的属性 */
                .filter(f -> !Modifier.isTransient(f.getModifiers()))
                .collect(Collectors.toList());

    }

    /**
     * <p>
     * 获取该类的所有getter函数
     * </p>
     *
     * @param clazz 反射类
     */
    public static Map<String, Method> getGetterMap(Class<?> clazz) {
        Method[] methods = clazz.getMethods();
        Map<String, Field> fieldMap = getFieldMap(clazz);
        Map<String, Method> result = new HashMap<>(fieldMap.size());
        for (Method method : methods) {
            String methodName = method.getName();
            if (!Modifier.isStatic(method.getModifiers())
                    && !methodName.equals(GET_CLASS)
                    && (methodName.startsWith(GET_PREFIX) || methodName.startsWith(IS_PREFIX))) {
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
     * 获取该类的所有setter函数
     * </p>
     *
     * @param clazz 反射类
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


    public static Object copy(Object source, Object target) {
        Map<String, Method> getterMap = getGetterMap(source.getClass());
        Map<String, Method> setterMap = getSetterMap(target.getClass());
        getterMap.forEach((k, v) -> {
            Method getterMethod = getterMap.get(k);
            Method setterMethod = setterMap.get(k);
            if (getterMethod != null && setterMethod != null) {
                try {
                    Object value = getterMethod.invoke(source);
                    Class<?> parameterType = setterMethod.getParameterTypes()[0];
                    if (value != null && parameterType.isAssignableFrom(value.getClass())) {
                        setterMethod.invoke(target, value);
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        });
        return target;
    }

    public static Class<?> getSuperClassGenericType(final Class<?> clazz, final Class<?> genericIfc, int index) {
        Class<?>[] classes = GenericTypeResolver.resolveTypeArguments(clazz, genericIfc);
        return classes == null ? null : classes[index];
    }

    /**
     * 获取lambda表达式对应的字段
     */
    public static <T, R> Field getField(AccessorFunction<T, R> accessorFunction) {

        // 从function取出序列化方法
        Method writeReplaceMethod;
        try {
            writeReplaceMethod = accessorFunction.getClass().getDeclaredMethod(WRITE_REPLACE);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        // 从序列化方法取出序列化的lambda信息
        boolean isAccessible = writeReplaceMethod.isAccessible();
        writeReplaceMethod.setAccessible(true);
        SerializedLambda serializedLambda;
        try {
            serializedLambda = (SerializedLambda) writeReplaceMethod.invoke(accessorFunction);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        writeReplaceMethod.setAccessible(isAccessible);

        // 从lambda信息取出method、field、class等
        String implMethodName = serializedLambda.getImplMethodName();
        // 确保方法是符合规范的get方法，boolean类型是is开头
        if (!implMethodName.startsWith(IS_PREFIX) && !implMethodName.startsWith(GET_PREFIX)) {
            throw new RuntimeException("Function: " + implMethodName + ",does not conform to the java bean specification");
        }

        // get方法开头为 is 或者 get，将方法名 去除is或者get，然后首字母小写，就是属性名
        int prefixLen = implMethodName.startsWith(IS_PREFIX) ? IS_PREFIX.length() : GET_PREFIX.length();

        String fieldName = StringKit.uncapitalize(implMethodName.substring(prefixLen));

        Field field;
        try {
            field = Class.forName(serializedLambda.getImplClass().replace(StringConstant.SLASH, StringConstant.DOT)).getDeclaredField(fieldName);
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        return field;
    }


}
