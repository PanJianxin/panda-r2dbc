package com.jxpanda.r2dbc.spring.data.core.kit;

import com.jxpanda.r2dbc.spring.data.config.R2dbcConfigProperties;
import com.jxpanda.r2dbc.spring.data.config.R2dbcEnvironment;
import com.jxpanda.r2dbc.spring.data.core.convert.R2dbcCustomTypeHandlers;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableEntity;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableId;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableLogic;
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
import org.springframework.data.util.Pair;
import org.springframework.data.util.ProxyUtils;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

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


    /**
     * 是否使用逻辑删除
     * 判定依据是：
     * 1、入参优先，入参的优先级最高
     * 2、类注解优先，如果类配置了${@link TableLogic}注解的enable属性为false，则不执行逻辑删除
     * 3、全局配置，如果没有配置注解，看全局是否配置逻辑删除，以全局的配置为主
     *
     * @param entityClass       entityClass
     * @param ignoreLogicDelete 是否忽略逻辑删除，这个的优先级高于所有配置
     */
    public static <T> boolean isLogicDeleteEnable(Class<T> entityClass, boolean ignoreLogicDelete) {

        if (ignoreLogicDelete) {
            return false;
        }

        RelationalPersistentEntity<T> requiredEntity = getRequiredEntity(entityClass);
        RelationalPersistentProperty logicDeleteProperty = requiredEntity.getPersistentProperty(TableLogic.class);
        if (logicDeleteProperty == null) {
            // 如果没有配置逻辑删除的字段，以全局配置为准
            R2dbcConfigProperties.LogicDelete logicDeleteConfig = R2dbcEnvironment.getLogicDelete();
            // 开启了逻辑删除配置，并且配置了逻辑删除字段才生效
            return logicDeleteConfig.enable() && !ObjectUtils.isEmpty(logicDeleteConfig.field());
        } else {
            // 如果配置了逻辑删除字段，以注解的配置为准
            TableLogic tableLogicAnnotation = logicDeleteProperty.getRequiredAnnotation(TableLogic.class);
            return tableLogicAnnotation.enable();
        }
    }

    /**
     * 获取对象中逻辑删除的字段和值
     *
     * @param entityClass entityClass
     * @return 第一个值是字段名，第二个值是逻辑删除的删除值
     */
    public static <T> Pair<String, Object> getLogicDeleteColumn(Class<T> entityClass, LogicDeleteValue whichValue) {
        RelationalPersistentEntity<T> requiredEntity = getRequiredEntity(entityClass);
        RelationalPersistentProperty logicDeleteProperty = requiredEntity.getPersistentProperty(TableLogic.class);
        // 默认取值是全局配置的逻辑删除字段
        R2dbcConfigProperties.LogicDelete logicDeleteConfig = R2dbcEnvironment.getLogicDelete();
        String logicDeleteField = logicDeleteConfig.field();
        Object value = switch (whichValue) {
            case DELETE_VALUE -> logicDeleteConfig.deleteValue();
            case UNDELETE_VALUE -> logicDeleteConfig.undeleteValue();
        };
        // 如果配置了注解，则以注解为准
        if (logicDeleteProperty != null) {
            logicDeleteField = logicDeleteProperty.getName();
            TableLogic tableLogicAnnotation = logicDeleteProperty.getRequiredAnnotation(TableLogic.class);
            value = switch (whichValue) {
                case DELETE_VALUE -> tableLogicAnnotation.deleteValue().getSupplier().get();
                case UNDELETE_VALUE -> tableLogicAnnotation.undeleteValue().getSupplier().get();
            };
        }

        if (value instanceof R2dbcConfigProperties.LogicDelete.Value customerValue) {
            value = customerValue.get();
        }

        return Pair.of(logicDeleteField, value);
    }


    public enum LogicDeleteValue {
        UNDELETE_VALUE,
        DELETE_VALUE
    }


}
