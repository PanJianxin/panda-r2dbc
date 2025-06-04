package com.jxpanda.r2dbc.spring.data.core.enhance.plugin;

import com.jxpanda.r2dbc.spring.data.config.R2dbcEnvironment;
import com.jxpanda.r2dbc.spring.data.config.properties.LogicDeletePluginProperties;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableLogic;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.annotation.R2dbcPluginSlot;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.contract.R2dbcCriteriaPlugin;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.contract.R2dbcUpdatePlugin;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model.R2dbcPluginEnum;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.value.LogicDeleteValue;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.value.LogicDeleteValueType;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.value.PluginValueHandler;
import com.jxpanda.r2dbc.spring.data.core.kit.QueryKit;
import com.jxpanda.r2dbc.spring.data.core.kit.R2dbcMappingKit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.query.Update;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Panda
 */
@R2dbcPluginSlot(values = {R2dbcPluginEnum.Slot.DELETE, R2dbcPluginEnum.Slot.SELECT})
public class R2dbcLogicDeletePlugin<T> extends R2dbcOperationPlugin implements R2dbcCriteriaPlugin<T>, R2dbcUpdatePlugin<T> {

    public R2dbcLogicDeletePlugin() {
        super(R2dbcPluginEnum.Name.LOGIC_DELETE);
    }

    public R2dbcLogicDeletePlugin(String pluginName) {
        this(pluginName, R2dbcPluginEnum.Name.LOGIC_DELETE.getDefaultSort());
    }

    public R2dbcLogicDeletePlugin(String pluginName, Integer sort) {
        super(pluginName, sort);
    }


    @Override
    public Mono<CriteriaDefinition> handleCriteria(Class<T> entityType, CriteriaDefinition original) {
        return QueryKit.criteriaMono(original)
                .map(criteriaDefinition -> {
                    CriteriaDefinition newCriteriaDefinition = criteriaDefinition;
                    if (isLogicDeleteEnable(entityType)) {
                        LogicDeleteMetadata undelete = getUndelete(entityType);
                        newCriteriaDefinition = QueryKit.mergeCriteria(criteriaDefinition, undelete.createCriteria());
                    }
                    return newCriteriaDefinition;
                });
    }

    @Override
    public Mono<Update> handleUpdate(Class<T> entityType, Update original) {
        return Mono.just(R2dbcLogicDeletePlugin.getDelete(entityType))
                .map(deleteMetadata -> {
                    if (original == null) {
                        return Update.update(deleteMetadata.field(), deleteMetadata.value());
                    }
                    return original.set(deleteMetadata.field(), deleteMetadata.value());
                });
    }


    /**
     * 是否使用逻辑删除
     * 判定依据是：
     * 1、入参优先，入参的优先级最高
     * 2、类注解优先，如果类配置了${@link TableLogic}注解的enable属性为false，则不执行逻辑删除
     * 3、全局配置，如果没有配置注解，看全局是否配置逻辑删除，以全局的配置为主
     *
     * @param entityType entityType
     */
    public static <T> boolean isLogicDeleteEnable(Class<T> entityType) {
        RelationalPersistentEntity<T> requiredEntity = R2dbcMappingKit.getRequiredEntity(entityType);
        RelationalPersistentProperty logicDeleteProperty = requiredEntity.getPersistentProperty(TableLogic.class);
        if (logicDeleteProperty == null) {
            // 如果没有配置逻辑删除的字段，以全局配置为准
            LogicDeletePluginProperties logicDeleteProperties = R2dbcEnvironment.getLogicDeleteProperties();
            // 开启了逻辑删除配置，并且配置了逻辑删除字段才生效
            return logicDeleteProperties.enable() && !ObjectUtils.isEmpty(logicDeleteProperties.field());
        } else {
            // 如果配置了逻辑删除字段，以注解的配置为准
            TableLogic tableLogicAnnotation = logicDeleteProperty.getRequiredAnnotation(TableLogic.class);
            return tableLogicAnnotation.enable();
        }
    }

    public static LogicDeleteMetadata getDelete(Class<?> entityClass) {
        return getLogicDeleteColumn(entityClass, WhichValue.DELETE_VALUE);
    }

    public static LogicDeleteMetadata getUndelete(Class<?> entityClass) {
        return getLogicDeleteColumn(entityClass, WhichValue.UNDELETE_VALUE);
    }

    /**
     * 获取对象中逻辑删除的字段和值
     *
     * @param entityClass entityClass
     * @return 第一个值是字段名，第二个值是逻辑删除的删除值
     */
    private static LogicDeleteMetadata getLogicDeleteColumn(Class<?> entityClass, WhichValue whichValue) {
        RelationalPersistentEntity<?> requiredEntity = R2dbcMappingKit.getRequiredEntity(entityClass);
        RelationalPersistentProperty logicDeleteProperty = requiredEntity.getPersistentProperty(TableLogic.class);
        // 默认取值是全局配置的逻辑删除字段
        LogicDeletePluginProperties logicDeleteProperties = R2dbcEnvironment.getLogicDeleteProperties();
        String logicDeleteField = logicDeleteProperties.field();
        Object value = whichValue.getValueFromProperties(logicDeleteProperties.value());
        LogicDeleteValueType logicDeleteValueType = logicDeleteProperties.value().getType();
        // 如果配置了注解，则以注解为准
        if (logicDeleteProperty != null) {
            logicDeleteField = logicDeleteProperty.getName();
            TableLogic tableLogicAnnotation = logicDeleteProperty.getRequiredAnnotation(TableLogic.class);
            value = whichValue.getValueFromAnnotation(tableLogicAnnotation);
            logicDeleteValueType = tableLogicAnnotation.type();
        }

        return new LogicDeleteMetadata(logicDeleteField, value, logicDeleteValueType);
    }


    public record LogicDeleteMetadata(String field, Object value, LogicDeleteValueType type) {

        public Criteria createCriteria() {
            return type.createCriteria(Criteria.where(field), value);
        }

    }

    @RequiredArgsConstructor
    public enum WhichValue {
        DELETE_VALUE(
                LogicDeleteValue::getDeleteValue,
                (tableLogic) -> tableLogic.type().getDeleteValue(),
                TableLogic::deleteValue,
                TableLogic::deleteValueHandler
        ),
        UNDELETE_VALUE(
                LogicDeleteValue::getUndeleteValue,
                (tableLogic) -> tableLogic.type().getUndeleteValue(),
                TableLogic::undeleteValue,
                TableLogic::undeleteValueHandler
        );

        private final Function<LogicDeleteValue, Object> valueFromProperties;
        private final Function<TableLogic, Supplier<Object>> valueSupplierFromAnnotation;
        private final Function<TableLogic, String> valueFromAnnotation;
        private final Function<TableLogic, Class<? extends PluginValueHandler>> valueHandlerFromAnnotation;

        private Object getValueFromProperties(LogicDeleteValue logicDeleteValue) {
            return valueFromProperties.apply(logicDeleteValue);
        }

        private Object getValueFromAnnotation(TableLogic tableLogic) {
            LogicDeleteValueType logicDeleteValueType = tableLogic.type();
            if (logicDeleteValueType == LogicDeleteValueType.CUSTOMER) {
                Class<? extends PluginValueHandler> handler = valueHandlerFromAnnotation.apply(tableLogic);
                String value = valueFromAnnotation.apply(tableLogic);
                return LogicDeleteValue.getValue(value, handler);
            } else {
                return valueSupplierFromAnnotation.apply(tableLogic).get();
            }
        }

    }


}
