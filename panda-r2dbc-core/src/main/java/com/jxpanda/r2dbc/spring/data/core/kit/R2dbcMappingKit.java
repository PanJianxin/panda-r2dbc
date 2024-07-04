package com.jxpanda.r2dbc.spring.data.core.kit;

import com.jxpanda.r2dbc.spring.data.config.R2dbcEnvironment;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.*;
import com.jxpanda.r2dbc.spring.data.core.enhance.handler.R2dbcCustomTypeHandlers;
import com.jxpanda.r2dbc.spring.data.core.enhance.strategy.ValidationStrategy;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.relational.repository.support.MappingRelationalEntityInformation;
import org.springframework.data.util.ProxyUtils;
import org.springframework.data.util.StreamUtils;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;

@Component
@AllArgsConstructor
public class R2dbcMappingKit {

    private final R2dbcCustomTypeHandlers typeHandlers;
    private final MappingContext<? extends RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> mappingContext;


    /**
     * 静态引用
     */
    private static R2dbcCustomTypeHandlers staticTypeHandlers;
    private static MappingContext<? extends RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> staticMappingContext;


    @PostConstruct
    private void init() {
        staticMappingContext = mappingContext;
        staticTypeHandlers = typeHandlers;
    }

    public static <T> SqlIdentifier getTableName(Class<T> entityClass) {
        return R2dbcMappingKit.getRequiredEntity(entityClass).getTableName();
    }

    public static <T> SqlIdentifier getTableNameOrEmpty(Class<T> entityClass) {

        RelationalPersistentEntity<T> entity = R2dbcMappingKit.getPersistentEntity(entityClass);

        return entity != null ? entity.getTableName() : SqlIdentifier.EMPTY;
    }

    @Nullable
    public static <T> RelationalPersistentEntity<T> getPersistentEntity(Class<T> entityClass) {
        return (RelationalPersistentEntity<T>) staticMappingContext.getPersistentEntity(entityClass);
    }

    @SuppressWarnings("unchecked")
    public static <T> RelationalPersistentEntity<T> getRequiredEntity(T entity) {
        return getRequiredEntity((Class<T>) ProxyUtils.getUserClass(entity));
    }

    public static <T> RelationalPersistentEntity<T> getRequiredEntity(Class<T> entityClass) {
        return (RelationalPersistentEntity<T>) staticMappingContext.getRequiredPersistentEntity(entityClass);
    }

    public static <T, ID> MappingRelationalEntityInformation<T, ID> getMappingRelationalEntityInformation(Class<T> entityClass, Class<ID> idClass) {
        return new MappingRelationalEntityInformation<>(getRequiredEntity(entityClass), idClass);
    }

    public static <E> boolean isAggregateEntity(Class<E> entityClass) {
        RelationalPersistentEntity<E> persistentEntity = getPersistentEntity(entityClass);
        return persistentEntity != null && isAggregateEntity(persistentEntity);
    }

    public static <E> boolean isAggregateEntity(RelationalPersistentEntity<E> relationalPersistentEntity) {
        TableEntity tableEntity = relationalPersistentEntity.findAnnotation(TableEntity.class);
        return tableEntity != null && tableEntity.aggregate();
    }

    public static <E> boolean isJoin(Class<E> entityClass) {
        RelationalPersistentEntity<E> persistentEntity = getPersistentEntity(entityClass);
        return persistentEntity != null && isJoin(persistentEntity);
    }

    public static <E> boolean isJoin(RelationalPersistentEntity<E> relationalPersistentEntity) {
        return relationalPersistentEntity.isAnnotationPresent(TableJoin.class);
    }

    public static <E> List<RelationalPersistentProperty> getReferenceProperties(Class<E> entityClass) {
        RelationalPersistentEntity<E> persistentEntity = getPersistentEntity(entityClass);
        return persistentEntity == null ? Collections.emptyList() : getReferenceProperties(persistentEntity);
    }

    public static <E> List<RelationalPersistentProperty> getReferenceProperties(RelationalPersistentEntity<E> relationalPersistentEntity) {
        return StreamUtils.createStreamFromIterator(relationalPersistentEntity.iterator())
                .filter(property -> property.isAnnotationPresent(TableReference.class))
                .toList();
    }


    /**
     * 返回字段是否是存在的
     * 主要用于排除虚拟字段
     */
    public static boolean isPropertyExists(@Nullable RelationalPersistentProperty property) {
        if (property == null) {
            return false;
        }
        if (property.isIdProperty()) {
            return true;
        }
        TableColumn tableColumn = property.findAnnotation(TableColumn.class);
        return tableColumn != null && tableColumn.exists();
    }

    public static boolean isFunctionProperty(RelationalPersistentProperty property) {
        TableColumn tableColumn = property.findAnnotation(TableColumn.class);
        return tableColumn != null && !ObjectUtils.isEmpty(tableColumn.function());
    }

    public static <T> Object getPropertyValue(T entity, RelationalPersistentEntity<T> relationalPersistentEntity, @Nullable RelationalPersistentProperty property) {

        if (property == null) {
            return null;
        }

        Object value;

        PersistentPropertyAccessor<T> accessor = relationalPersistentEntity.getPropertyAccessor(entity);
        if (property.isIdProperty()) {
            IdentifierAccessor identifierAccessor = relationalPersistentEntity.getIdentifierAccessor(accessor.getBean());
            value = identifierAccessor.getIdentifier();
        } else if (staticTypeHandlers.hasTypeHandler(property)) {
            value = staticTypeHandlers.write(accessor.getProperty(property), property);
        } else {
            value = accessor.getProperty(property);
        }
        return value;
    }

    public static <T> boolean isPropertyEffective(T entity, RelationalPersistentEntity<T> relationalPersistentEntity, @Nullable RelationalPersistentProperty property) {
        return isPropertyEffective(relationalPersistentEntity, property, getPropertyValue(entity, relationalPersistentEntity, property));
    }

    /**
     * 返回字段的值是否有效
     * 处理空值，在插入/更新数据的时候判定是否需要过滤掉对应字段的判别依据
     */
    public static <T> boolean isPropertyEffective(RelationalPersistentEntity<T> entity, @Nullable RelationalPersistentProperty property, @Nullable Object value) {

        // 如果字段不存在，直接返回false
        if (property == null) {
            return false;
        }
        // 判别优先级
        // 字段上的配置 > 类上的配置 > 全局配置文件的配置

        // 默认是全局校验策略
        ValidationStrategy validationStrategy = R2dbcEnvironment.getMapping().validationStrategy();

        // 类上面的校验策略
        TableEntity tableEntity = entity.findAnnotation(TableEntity.class);
        if (tableEntity != null && tableEntity.validationPolicy() != ValidationStrategy.DEFAULT) {
            validationStrategy = tableEntity.validationPolicy();
        }

        // id的校验策略
        TableId tableId = property.findAnnotation(TableId.class);
        if (tableId != null && tableId.validationPolicy() != ValidationStrategy.DEFAULT) {
            validationStrategy = tableId.validationPolicy();
        }

        // 字段上的校验策略
        TableColumn tableColumn = property.findAnnotation(TableColumn.class);
        if (tableColumn != null && tableColumn.validationPolicy() != ValidationStrategy.DEFAULT) {
            validationStrategy = tableColumn.validationPolicy();
        }

        // 使用策略判定字段是否有效
        return validationStrategy.isEffective(value);
    }







}
