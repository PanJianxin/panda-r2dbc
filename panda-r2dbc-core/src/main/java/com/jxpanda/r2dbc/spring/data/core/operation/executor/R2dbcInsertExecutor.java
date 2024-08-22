package com.jxpanda.r2dbc.spring.data.core.operation.executor;

import com.jxpanda.r2dbc.spring.data.config.R2dbcEnvironment;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableId;
import com.jxpanda.r2dbc.spring.data.core.enhance.strategy.IdStrategy;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;


@SuppressWarnings("deprecation")
public class R2dbcInsertExecutor<T> extends R2dbcOperationExecutor.WriteExecutor<T, T> {

    private final BiFunction<R2dbcOperationParameter<T, T>, OutboundRow, StatementMapper.InsertSpec> specBuilder;

    private final BiFunction<R2dbcOperationParameter<T, T>, StatementMapper.InsertSpec, PreparedOperation<?>> preparedOperationBuilder;

    private R2dbcInsertExecutor(R2dbcOperationParameter<T, T> operationParameter,
                                Function<R2dbcOperationParameter<T, T>, Query> queryHandler) {
        super(operationParameter, queryHandler);
        this.specBuilder = defaultSpecBuilder();
        this.preparedOperationBuilder = (parameter, insertSpec) -> parameter.getStatementMapper().getMappedObject(insertSpec);
    }


    public static <T> R2dbcInsertExecutorBuilder<T> builder() {
        return new R2dbcInsertExecutorBuilder<>();
    }

    @Override
    protected Mono<T> fetch(R2dbcOperationParameter<T, T> parameter) {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    @Override
    protected Mono<T> fetch(T domainEntity, R2dbcOperationParameter<T, T> parameter) {
        SqlIdentifier tableName = parameter.getTableName();
        RelationalPersistentEntity<T> persistentEntity = parameter.getRelationalPersistentEntity();
        return template().maybeCallBeforeConvert(domainEntity, tableName)
                .flatMap(onBeforeConvert -> {
                    T initializedEntity = setVersionIfNecessary(persistentEntity, onBeforeConvert);
                    // id生成处理
                    potentiallyGeneratorId(persistentEntity.getPropertyAccessor(domainEntity), persistentEntity.getIdProperty());
                    OutboundRow outboundRow = getOutboundRow(initializedEntity);
                    potentiallyRemoveId(persistentEntity, outboundRow);
                    return template().maybeCallBeforeSave(initializedEntity, outboundRow, tableName)
                            .flatMap(entityToSave -> {
                                StatementMapper.InsertSpec insertSpec = specBuilder.apply(parameter, outboundRow);
                                PreparedOperation<?> operation = preparedOperationBuilder.apply(parameter, insertSpec);
                                List<SqlIdentifier> identifierColumns = getIdentifierColumns(domainEntity.getClass());
                                return this.databaseClient().sql(operation)
                                        .filter(statement -> {
                                            if (identifierColumns.isEmpty()) {
                                                return statement.returnGeneratedValues();
                                            }
                                            return statement.returnGeneratedValues(dataAccessStrategy().renderForGeneratedValues(identifierColumns.get(0)));
                                        })
                                        .map(converter().populateIdIfNecessary(domainEntity))
                                        .all().last(domainEntity)
                                        .flatMap(saved -> template().maybeCallAfterSave(saved, outboundRow, tableName));
                            });
                });
    }


    private BiFunction<R2dbcOperationParameter<T, T>, OutboundRow, StatementMapper.InsertSpec> defaultSpecBuilder() {
        return (parameter, outboundRow) -> {
            StatementMapper statementMapper = parameter.getStatementMapper();
            SqlIdentifier tableName = parameter.getTableName();
            StatementMapper.InsertSpec insertSpec = statementMapper.createInsert(tableName);
            for (Map.Entry<SqlIdentifier, Parameter> entry : outboundRow.entrySet()) {
                if (entry.getValue().hasValue()) {
                    insertSpec = insertSpec.withColumn(entry.getKey(), entry.getValue());
                }
            }
            return insertSpec;
        };
    }

    private List<SqlIdentifier> getIdentifierColumns(Class<?> clazz) {
        return dataAccessStrategy().getIdentifierColumns(clazz);
    }

    private void potentiallyGeneratorId(PersistentPropertyAccessor<?> propertyAccessor, @Nullable RelationalPersistentProperty idProperty) {
        if (idProperty == null) {
            return;
        }
        // 检查一下是否主动传递了id字段，没有传递的时候才生成
        Object idValue = propertyAccessor.getProperty(idProperty);
        // 如果id没有主动且策略是生成策略才生成id
        if (idValue == null && shouldGeneratorIdValue(idProperty)) {
            Object generatedIdValue = idGenerator().generate();
            ConversionService conversionService = converter().getConversionService();
            propertyAccessor.setProperty(idProperty, conversionService.convert(generatedIdValue, idProperty.getType()));
        }
    }

    private void potentiallyRemoveId(RelationalPersistentEntity<?> persistentEntity, OutboundRow outboundRow) {
        RelationalPersistentProperty idProperty = persistentEntity.getIdProperty();
        if (idProperty == null) {
            return;
        }
        SqlIdentifier columnName = idProperty.getColumnName();
        Parameter parameter = outboundRow.get(columnName);
        if (shouldSkipIdValue(parameter)) {
            outboundRow.remove(columnName);
        }
    }

    private boolean shouldSkipIdValue(@Nullable Parameter value) {
        if (value == null || value.getValue() == null) {
            return true;
        }
        if (value.getValue() instanceof Number numberValue) {
            return numberValue.longValue() == 0L;
        }
        return false;
    }

    /**
     * 返回是否需要生成id
     * 基于IdStrategy的配置来判断
     *
     * @param idProperty idProperty
     */
    private boolean shouldGeneratorIdValue(RelationalPersistentProperty idProperty) {
        IdStrategy idStrategy = R2dbcEnvironment.getDatabaseProperties().idStrategy();
        TableId tableId = idProperty.findAnnotation(TableId.class);
        if (tableId != null) {
            idStrategy = tableId.idStrategy() == IdStrategy.DEFAULT ? idStrategy : tableId.idStrategy();
        }
        return idStrategy == IdStrategy.USE_GENERATOR;
    }


    @SuppressWarnings("unchecked")
    <E> E setVersionIfNecessary(RelationalPersistentEntity<E> persistentEntity, E entity) {
        RelationalPersistentProperty versionProperty = persistentEntity.getVersionProperty();
        if (versionProperty == null) {
            return entity;
        }
        Class<?> versionPropertyType = versionProperty.getType();
        Long version = versionPropertyType.isPrimitive() ? 1L : 0L;
        ConversionService conversionService = this.converter().getConversionService();
        PersistentPropertyAccessor<?> propertyAccessor = persistentEntity.getPropertyAccessor(entity);
        propertyAccessor.setProperty(versionProperty, conversionService.convert(version, versionPropertyType));
        return (E) propertyAccessor.getBean();
    }

    public static final class R2dbcInsertExecutorBuilder<T> extends R2dbcOperationExecutor.R2dbcExecutorBuilder<T, T, R2dbcInsertExecutor<T>, R2dbcInsertExecutorBuilder<T>> {
        public R2dbcInsertExecutor<T> buildExecutor() {
            return new R2dbcInsertExecutor<>(operationParameter, queryHandler);
        }

        @Override
        protected R2dbcInsertExecutorBuilder<T> self() {
            return this;
        }

    }

}
