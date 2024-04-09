package com.jxpanda.r2dbc.spring.data.extension.service;

import com.jxpanda.r2dbc.spring.data.core.kit.R2dbcMappingKit;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.ReflectionKit;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Update;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author Panda
 */
public class ServiceHelper {

    /**
     * 工具类，禁用构造方法
     */
    private ServiceHelper() {

    }

    /**
     * 构建一个更新操作对象，基于提供的实体对象及其属性的改变。
     *
     * @param entity 要构建更新操作的对象。
     * @param <T>    实体对象的类型。
     * @return 返回一个包含更新信息的Update对象。
     */
    public static <T> Update buildUpdate(T entity) {
        return buildUpdate(entity, columInfo -> columInfo.getValue() != null);
    }

    /**
     * 根据指定条件构建一个更新操作对象，基于提供的实体对象及其属性的改变。
     *
     * @param entity    要构建更新操作的对象。
     * @param predicate 用于确定哪些属性应该被包含在更新操作中的条件函数。
     * @param <T>       实体对象的类型。
     * @return 返回一个包含更新信息的Update对象。
     */
    public static <T> Update buildUpdate(T entity, Predicate<ColumInfo> predicate) {
        Update update = Update.from(new HashMap<>());
        forEachColum(entity, columInfo -> {
            if (predicate.test(columInfo)) {
                update.set(columInfo.getColumName(), columInfo.getValue());
            }
        });
        return update;
    }

    /**
     * 遍历实体对象的属性，并对每个属性执行指定的操作。
     *
     * @param entity   要遍历属性的实体对象。
     * @param consumer 对每个属性执行的操作。
     * @param <T>      实体对象的类型。
     */
    public static <T> void forEachColum(T entity, Consumer<ColumInfo> consumer) {
        RelationalPersistentEntity<T> requiredPersistentEntity = R2dbcMappingKit.getRequiredEntity(entity);
        requiredPersistentEntity.forEach(property -> {
            try {
                if (property != null && property.getGetter() != null) {
                    consumer.accept(new ColumInfo(property, property.getGetter().invoke(entity)));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }


    /**
     * 包含实体属性信息的类。
     */
    @Getter
    @ToString
    public static class ColumInfo {

        /**
         * 字段名
         */
        private final String fieldName;
        /**
         * 数据库列名
         */
        private final String columName;
        /**
         * 别名
         */
        private final String alias;
        /**
         * 字段值
         */
        private final Object value;

        /**
         * 构造函数。
         *
         * @param property 实体属性信息。
         * @param value    属性的值。
         */
        private ColumInfo(RelationalPersistentProperty property, Object value) {
            this.fieldName = property.getName();
            this.value = value;
            this.alias = "";
            this.columName = property.getColumnName().getReference();
        }

    }

    /**
     * 获取类的ID类型。
     *
     * @param clazz 要获取ID类型的类。
     * @param <ID>  ID的类型。
     * @return 返回ID类型的Class对象。
     */
    public static <ID> Class<ID> takeIdClass(Class<?> clazz) {
        return takeGenericType(clazz, 1);
    }

    /**
     * 获取实体类的泛型类型。
     *
     * @param clazz 要获取泛型类型的类。
     * @param <T>   实体类的类型。
     * @return 返回实体类的泛型类型Class对象。
     */
    public static <T> Class<T> takeEntityClass(Class<?> clazz) {
        return takeGenericType(clazz, 0);
    }

    /**
     * 获取类的泛型类型。
     *
     * @param clazz 要获取泛型类型的类。
     * @param index 泛型参数的索引。
     * @param <X>   泛型类型。
     * @return 返回指定索引的泛型类型Class对象。
     */
    @SuppressWarnings("unchecked")
    public static <X> Class<X> takeGenericType(Class<?> clazz, int index) {
        return (Class<X>) ReflectionKit.getSuperClassGenericType(clazz, Service.class, index);
    }


}
