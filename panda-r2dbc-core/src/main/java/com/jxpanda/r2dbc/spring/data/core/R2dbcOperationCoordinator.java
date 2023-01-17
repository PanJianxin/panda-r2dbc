package com.jxpanda.r2dbc.spring.data.core;

import com.jxpanda.r2dbc.spring.data.config.R2dbcConfigProperties;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableLogic;
import com.jxpanda.r2dbc.spring.data.core.enhance.key.IdGenerator;
import com.jxpanda.r2dbc.spring.data.core.query.LambdaCriteria;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.util.Pair;
import org.springframework.data.util.ProxyUtils;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.ObjectUtils;

import java.util.Optional;

@SuppressWarnings({"unchecked", "rawtypes", "unused", "SameParameterValue"})
public class R2dbcOperationCoordinator {


    private final ReactiveEntityTemplate template;

    R2dbcOperationCoordinator(ReactiveEntityTemplate template) {
        this.template = template;
    }

    ReactiveEntityTemplate getTemplate() {
        return template;
    }

    R2dbcConfigProperties r2dbcConfigProperties() {
        return this.getTemplate().getR2dbcConfigProperties();
    }

    SpelAwareProxyProjectionFactory projectionFactory() {
        return this.getTemplate().getProjectionFactory();
    }

    MappingContext<? extends RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> mappingContext() {
        return this.getTemplate().getConverter().getMappingContext();
    }

    StatementMapper statementMapper() {
        return this.getTemplate().getDataAccessStrategy().getStatementMapper();
    }

    R2dbcConverter converter() {
        return this.getTemplate().getConverter();
    }

    DatabaseClient databaseClient() {
        return this.getTemplate().getDatabaseClient();
    }

    IdGenerator<?> idGenerator() {
        return this.getTemplate().getIdGenerator();
    }

    TransactionalOperator transactionalOperator() {
        return this.getTemplate().getTransactionalOperator();
    }

    TransactionalOperator transactionalOperator(int propagationBehavior, int isolationLevel, int timeout, boolean readOnly) {
        return TransactionalOperator.create(this.getTemplate().getR2dbcTransactionManager(), new TransactionDefinition() {
            @Override
            public int getPropagationBehavior() {
                return propagationBehavior;
            }

            @Override
            public int getIsolationLevel() {
                return isolationLevel;
            }

            @Override
            public int getTimeout() {
                return timeout;
            }

            @Override
            public boolean isReadOnly() {
                return readOnly;
            }
        });
    }

    <T> SqlIdentifier getTableName(Class<T> entityClass) {
        return getRequiredEntity(entityClass).getTableName();
    }

    <T> SqlIdentifier getTableNameOrEmpty(Class<T> entityClass) {

        RelationalPersistentEntity<T> entity = getPersistentEntity(entityClass);

        return entity != null ? entity.getTableName() : SqlIdentifier.EMPTY;
    }


    @Nullable
    <T> RelationalPersistentEntity<T> getPersistentEntity(Class<T> entityClass) {
        return (RelationalPersistentEntity<T>) mappingContext().getPersistentEntity(entityClass);
    }

    <T> RelationalPersistentEntity<T> getRequiredEntity(T entity) {
        Class<?> entityType = ProxyUtils.getUserClass(entity);
        return (RelationalPersistentEntity) getRequiredEntity(entityType);
    }

    <T> RelationalPersistentEntity<T> getRequiredEntity(Class<T> entityClass) {
        return (RelationalPersistentEntity<T>) mappingContext().getRequiredPersistentEntity(entityClass);
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
    <T> boolean isLogicDeleteEnable(Class<T> entityClass, boolean ignoreLogicDelete) {

        if (ignoreLogicDelete) {
            return false;
        }

        RelationalPersistentEntity<T> requiredEntity = getRequiredEntity(entityClass);
        RelationalPersistentProperty logicDeleteProperty = requiredEntity.getPersistentProperty(TableLogic.class);
        if (logicDeleteProperty == null) {
            // 如果没有配置逻辑删除的字段，以全局配置为准
            R2dbcConfigProperties.LogicDelete logicDeleteConfig = r2dbcConfigProperties().logicDelete();
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
    <T> Pair<String, Object> getLogicDeleteColumn(Class<T> entityClass, LogicDeleteValue whichValue) {
        RelationalPersistentEntity<T> requiredEntity = getRequiredEntity(entityClass);
        RelationalPersistentProperty logicDeleteProperty = requiredEntity.getPersistentProperty(TableLogic.class);
        // 默认取值是全局配置的逻辑删除字段
        R2dbcConfigProperties.LogicDelete logicDeleteConfig = r2dbcConfigProperties().logicDelete();
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

    /**
     * 创建逻辑删除的Update对象
     *
     * @param entityClass entityClass
     */
    <T> Update createLogicDeleteUpdate(Class<T> entityClass) {
        Pair<String, Object> logicDeleteColumn = getLogicDeleteColumn(entityClass, LogicDeleteValue.DELETE_VALUE);
        return Update.update(logicDeleteColumn.getFirst(), logicDeleteColumn.getSecond());
    }

    <T> StatementMapper.SelectSpec selectWithCriteria(StatementMapper.SelectSpec selectSpec, Query query, Class<T> entityClass, boolean ignoreLogicDelete) {
        Optional<CriteriaDefinition> criteriaOptional = query.getCriteria();
        if (isLogicDeleteEnable(entityClass, ignoreLogicDelete)) {
            criteriaOptional = query.getCriteria()
                    .or(() -> Optional.of(Criteria.empty()))
                    .map(criteriaDefinition -> {
                        // 获取查询对象中的逻辑删除字段和值，写入到criteria中
                        Pair<String, Object> logicDeleteColumn = getLogicDeleteColumn(entityClass, LogicDeleteValue.UNDELETE_VALUE);
                        if (criteriaDefinition instanceof Criteria criteria) {
                            return criteria.and(Criteria.where(logicDeleteColumn.getFirst()).is(logicDeleteColumn.getSecond()));
                        }
                        if (criteriaDefinition instanceof LambdaCriteria lambdaCriteria) {
                            return lambdaCriteria.and(LambdaCriteria.where(logicDeleteColumn.getFirst()).is(logicDeleteColumn.getSecond()));
                        }
                        return criteriaDefinition;
                    });
        }
        return criteriaOptional.map(selectSpec::withCriteria).orElse(selectSpec);
    }

    enum LogicDeleteValue {
        UNDELETE_VALUE,
        DELETE_VALUE
    }


}
