package com.jxpanda.r2dbc.spring.data.core.enhance.plugin;

import com.jxpanda.r2dbc.spring.data.config.R2dbcEnvironment;
import com.jxpanda.r2dbc.spring.data.config.properties.LogicDeletePluginProperties;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableLogic;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.value.LogicDeleteValue;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.value.LogicDeleteValueType;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.value.PluginValueHandler;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.criteria.EnhancedCriteria;
import com.jxpanda.r2dbc.spring.data.core.kit.R2dbcMappingKit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.query.Update;
import org.springframework.data.util.Pair;
import org.springframework.util.ObjectUtils;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Panda
 */
public class R2DbcLogicDeletePlugin extends R2dbcOperationPlugin {

    public R2DbcLogicDeletePlugin() {
        super(R2dbcPluginName.LOGIC_DELETE.getGroup(), R2dbcPluginName.LOGIC_DELETE.getName());
    }

    @Override
    protected <T, R, PR> R2dbcPluginContext<T, R, PR> plugInto(R2dbcPluginContext<T, R, PR> context) {
        if (Update.class.equals(context.getPluginResultType())) {
            return context.<Update>execute(this::handleUpdate);
        }
        if (CriteriaDefinition.class.equals(context.getPluginResultType())) {
            return context.<CriteriaDefinition>execute(this::handleCriteria);
        }
        return context;
    }

    private <T, R> Update handleUpdate(R2dbcPluginContext<T, R, Update> context) {
        Class<T> domainType = context.getDomainType();
        Pair<String, Object> logicDeleteColumn = R2DbcLogicDeletePlugin.getDelete(domainType);
        return Update.update(logicDeleteColumn.getFirst(), logicDeleteColumn.getSecond());
    }

    private <T, R> CriteriaDefinition handleCriteria(R2dbcPluginContext<T, R, CriteriaDefinition> context) {
        Class<T> domainType = context.getDomainType();
        CriteriaDefinition criteriaDefinition = context.getLastPluginResult() == null ? Criteria.empty() : context.getLastPluginResult();
        if (isLogicDeleteEnable(domainType)) {
            // 获取查询对象中的逻辑删除字段和值，写入到criteria中
            Pair<String, Object> logicDeleteColumn = getUndelete(domainType);
            if (criteriaDefinition instanceof Criteria criteria) {
                return criteria.and(Criteria.where(logicDeleteColumn.getFirst()).is(logicDeleteColumn.getSecond()));
            }
            if (criteriaDefinition instanceof EnhancedCriteria enhancedCriteria) {
                return enhancedCriteria.and(EnhancedCriteria.where(logicDeleteColumn.getFirst()).is(logicDeleteColumn.getSecond()));
            }
        }
        return criteriaDefinition;
    }

    /**
     * 是否使用逻辑删除
     * 判定依据是：
     * 1、入参优先，入参的优先级最高
     * 2、类注解优先，如果类配置了${@link TableLogic}注解的enable属性为false，则不执行逻辑删除
     * 3、全局配置，如果没有配置注解，看全局是否配置逻辑删除，以全局的配置为主
     *
     * @param domainType domainType
     */
    public static <T> boolean isLogicDeleteEnable(Class<T> domainType) {
        RelationalPersistentEntity<T> requiredEntity = R2dbcMappingKit.getRequiredEntity(domainType);
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

    public static <T> Pair<String, Object> getDelete(Class<T> entityClass) {
        return getLogicDeleteColumn(entityClass, WhichValue.DELETE_VALUE);
    }

    public static <T> Pair<String, Object> getUndelete(Class<T> entityClass) {
        return getLogicDeleteColumn(entityClass, WhichValue.UNDELETE_VALUE);
    }

    /**
     * 获取对象中逻辑删除的字段和值
     *
     * @param entityClass entityClass
     * @return 第一个值是字段名，第二个值是逻辑删除的删除值
     */
    private static <T> Pair<String, Object> getLogicDeleteColumn(Class<T> entityClass, WhichValue whichValue) {
        RelationalPersistentEntity<T> requiredEntity = R2dbcMappingKit.getRequiredEntity(entityClass);
        RelationalPersistentProperty logicDeleteProperty = requiredEntity.getPersistentProperty(TableLogic.class);
        // 默认取值是全局配置的逻辑删除字段
        LogicDeletePluginProperties logicDeleteProperties = R2dbcEnvironment.getLogicDeleteProperties();
        String logicDeleteField = logicDeleteProperties.field();
        Object value = whichValue.getValueFromProperties(logicDeleteProperties.value());
        // 如果配置了注解，则以注解为准
        if (logicDeleteProperty != null) {
            logicDeleteField = logicDeleteProperty.getName();
            TableLogic tableLogicAnnotation = logicDeleteProperty.getRequiredAnnotation(TableLogic.class);
            value = whichValue.getValueFromAnnotation(tableLogicAnnotation);
        }

        return Pair.of(logicDeleteField, value);
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
