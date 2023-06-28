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

    static <T> Update buildUpdate(T entity) {
        return buildUpdate(entity, columInfo -> columInfo.getValue() != null);
    }

    static <T> Update buildUpdate(T entity, Predicate<ColumInfo> predicate) {
        Update update = Update.from(new HashMap<>());
        forEachColum(entity, columInfo -> {
            if (predicate.test(columInfo)) {
                update.set(columInfo.getColumName(), columInfo.getValue());
            }
        });
        return update;
    }

    static <T> void forEachColum(T entity, Consumer<ColumInfo> consumer) {
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


    @Getter
    @ToString
    static class ColumInfo {

        private final String fieldName;
        private final String columName;
        private final String alias;
        private final Object value;

        private ColumInfo(RelationalPersistentProperty property, Object value) {
            this.fieldName = property.getName();
            this.value = value;
            this.alias = "";
            this.columName = property.getColumnName().getReference();
        }

    }

    static <ID> Class<ID> takeIdClass(Class<?> clazz) {
        return takeGenericType(clazz, 1);
    }

    static <T> Class<T> takeEntityClass(Class<?> clazz) {
        return takeGenericType(clazz, 0);
    }

    @SuppressWarnings("unchecked")
    private static <X> Class<X> takeGenericType(Class<?> clazz, int index) {
        return (Class<X>) ReflectionKit.getSuperClassGenericType(clazz, Service.class, index);
    }

}
