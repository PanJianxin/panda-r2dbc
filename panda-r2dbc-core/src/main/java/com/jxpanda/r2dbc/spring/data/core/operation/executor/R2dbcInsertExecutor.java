package com.jxpanda.r2dbc.spring.data.core.operation.executor;

import com.jxpanda.r2dbc.spring.data.config.R2dbcEnvironment;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableId;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model.R2dbcPluginContext;
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
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;


@SuppressWarnings("deprecation")
public class R2dbcInsertExecutor<T> extends R2dbcOperationExecutor.WriteExecutor<T, T> {

    private final BiFunction<R2dbcOperationContext<T, T>, OutboundRow, StatementMapper.InsertSpec> specBuilder;

    private final BiFunction<R2dbcOperationContext<T, T>, StatementMapper.InsertSpec, PreparedOperation<?>> preparedOperationBuilder;

    private R2dbcInsertExecutor(R2dbcOperationContext<T, T> operationContext,
                                Function<R2dbcOperationContext<T, T>, Query> queryHandler) {
        super(operationContext, queryHandler);
        this.specBuilder = defaultSpecBuilder();
        this.preparedOperationBuilder = (context, insertSpec) -> context.getStatementMapper().getMappedObject(insertSpec);
    }


    public static <T> R2dbcInsertExecutorBuilder<T> builder() {
        return new R2dbcInsertExecutorBuilder<>();
    }

    @Override
    protected Mono<T> fetch(R2dbcOperationContext<T, T> operationContext) {
        return fetchBefore(operationContext)
                .flatMap(tuple -> {
                    R2dbcPluginContext<T, T> pluginContext = tuple.getT1();
                    T entity = Objects.requireNonNull(pluginContext.getEntity());
                    StatementMapper.InsertSpec insertSpec = specBuilder.apply(operationContext, tuple.getT2());
                    PreparedOperation<?> operation = preparedOperationBuilder.apply(operationContext, insertSpec);
                    List<SqlIdentifier> identifierColumns = getIdentifierColumns(entity.getClass());
                    return this.databaseClient().sql(operation)
                            .filter(statement -> {
                                statement = template().getStatementFilterFunction().apply(statement);
                                if (identifierColumns.isEmpty()) {
                                    return statement.returnGeneratedValues();
                                }
                                return statement.returnGeneratedValues(dataAccessStrategy().renderForGeneratedValues(identifierColumns.get(0)));
                            })
                            .map(converter().populateIdIfNecessary(entity))
                            .all().last(entity)
                            .flatMap(savedEntity -> fetchAfter(operationContext, tuple.getT2(), pluginContext.withResult(this, savedEntity)));
                });
    }

    private Mono<Tuple2<R2dbcPluginContext<T, T>, OutboundRow>> fetchBefore(R2dbcOperationContext<T, T> operationContext) {
        return template().maybeCallBeforeConvert(operationContext.getEntity(), operationContext.getTableName())
                .flatMap(onConvertEntity -> {
                    RelationalPersistentEntity<T> persistentEntity = operationContext.getRelationalPersistentEntity();
                    T initializedEntity = setVersionIfNecessary(persistentEntity, onConvertEntity);
                    // id生成处理
                    // TODO: 增加了插件层之后，id的生成以及乐观锁的注入都可以考虑整合到插件层中，以后改
                    potentiallyGeneratorId(persistentEntity.getPropertyAccessor(onConvertEntity), persistentEntity.getIdProperty());
                    OutboundRow outboundRow = getOutboundRow(initializedEntity);
                    potentiallyRemoveId(persistentEntity, outboundRow);
                    return template()
                            .maybeCallBeforeSave(initializedEntity, outboundRow, operationContext.getTableName())
                            .zipWith(Mono.just(outboundRow));
                })
                .flatMap(tuple -> pluginExecutor()
                        .runBefore(operationContext.createPluginContext(this, tuple.getT1()))
                        .map(pluginContext -> {
                            OutboundRow outboundRow = tuple.getT2();
                            // 合并一下，后续生成SQL实际上是用这个对象生成的，和entity没关系了，如果不合并，插件等于没执行
                            // 也不能强行替换，因为万一maybeCallBeforeSave这里有逻辑，改过OutboundRow对象，就失效了
                            outboundRow.putAll(getOutboundRow(tuple.getT1()));
                            return Tuples.of(pluginContext, outboundRow);
                        }));
    }

    private Mono<T> fetchAfter(R2dbcOperationContext<T, T> operationContext, OutboundRow outboundRow, R2dbcPluginContext<T, T> context) {
        return template().maybeCallAfterSave(context.getResult(), outboundRow, operationContext.getTableName())
                .flatMap(savedEntity -> pluginExecutor().runAfter(context.withResult(this, savedEntity)))
                .mapNotNull(R2dbcPluginContext::getResult)
                .doOnSuccess(operationContext::withResult);
    }

    private BiFunction<R2dbcOperationContext<T, T>, OutboundRow, StatementMapper.InsertSpec> defaultSpecBuilder() {
        return (operationContext, outboundRow) -> {
            StatementMapper statementMapper = operationContext.getStatementMapper();
            SqlIdentifier tableName = operationContext.getTableName();
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
        Parameter operationContext = outboundRow.get(columnName);
        if (shouldSkipIdValue(operationContext)) {
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
            return new R2dbcInsertExecutor<>(operationContext, queryHandler);
        }

        @Override
        protected R2dbcInsertExecutorBuilder<T> self() {
            return this;
        }

    }

}
