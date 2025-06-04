package com.jxpanda.r2dbc.spring.data.core.operation.executor;

import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model.R2dbcPluginContext;
import com.jxpanda.r2dbc.spring.data.core.kit.QueryKit;
import com.jxpanda.r2dbc.spring.data.core.kit.R2dbcMappingKit;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.ReflectionKit;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.r2dbc.core.PreparedOperation;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings({"deprecation", "unchecked", "rawtypes"})
public class R2dbcUpdateExecutor<T, R> extends R2dbcOperationExecutor.WriteExecutor<T, R> {

    private final Supplier<Update> updateSupplier;

    private R2dbcUpdateExecutor(R2dbcOperationContext<T, R> operationContext,
                                Function<R2dbcOperationContext<T, R>, Query> queryHandler,
                                Supplier<Update> updateSupplier
    ) {
        super(operationContext, queryHandler);
        this.updateSupplier = updateSupplier;
    }

    public static <T, R> R2dbcUpdateExecutorBuilder<T, R> builder() {
        return new R2dbcUpdateExecutorBuilder<>();
    }



    private Mono<Tuple2<R2dbcPluginContext<T, R>, Optional<OutboundRow>>> fetchBefore(R2dbcOperationContext<T, R> operationContext) {
        return Mono.just(operationContext)
                .flatMap(it -> {
                    if (operationContext.getEntity() == null) {
                        return Mono.just(Tuples.of(updateSupplier.get(), operationContext.getQuery().getCriteria().orElse(Criteria.empty()), Optional.<OutboundRow>empty()));
                    }
                    return fetchBeforeWithEntity(operationContext);
                })
                .flatMap(tuple -> {
                    R2dbcPluginContext<T, R> pluginContext = operationContext.createPluginContext(this)
                            .rebuilder()
                            .update(tuple.getT1())
                            .criteria(tuple.getT2())
                            .build();
                    return pluginExecutor().runBefore(pluginContext)
                            .map(it -> Tuples.of(it, tuple.getT3()));
                });
    }

    private Mono<Tuple3<Update, CriteriaDefinition, Optional<OutboundRow>>> fetchBeforeWithEntity(R2dbcOperationContext<T, R> operationContext) {
        T entity = operationContext.getEntity();
        RelationalPersistentEntity<T> persistentEntity = Objects.requireNonNull(operationContext.getRelationalPersistentEntity());
        SqlIdentifier tableName = operationContext.getTableName();
        return template().maybeCallBeforeConvert(entity, tableName)
                .map(onBeforeConvertEntity -> {
                    T entityToUse;
                    Criteria matchingVersionCriteria;

                    if (persistentEntity.hasVersionProperty()) {
                        matchingVersionCriteria = createMatchingVersionCriteria(onBeforeConvertEntity, persistentEntity);
                        entityToUse = incrementVersion(persistentEntity, onBeforeConvertEntity);
                    } else {
                        entityToUse = onBeforeConvertEntity;
                        matchingVersionCriteria = null;
                    }

                    OutboundRow outboundRow = getOutboundRow(entityToUse);
                    return Tuples.of(entityToUse, outboundRow, Optional.ofNullable(matchingVersionCriteria));
                })
                .flatMap(tuple -> template().maybeCallBeforeSave(tuple.getT1(), tuple.getT2(), tableName)
                        .map(onBeforeSaveEntity -> Tuples.of(onBeforeSaveEntity, tuple.getT2(), tuple.getT3())))
                .map(tuple -> {
                    T onBeforeSaveEntity = tuple.getT1();
                    SqlIdentifier idColumn = persistentEntity.getRequiredIdProperty().getColumnName();
                    OutboundRow outboundRow = tuple.getT2();
                    Parameter id = outboundRow.remove(idColumn);

                    persistentEntity.forEach(property -> {
                        if (property.isInsertOnly() || !R2dbcMappingKit.isPropertyEffective(onBeforeSaveEntity, persistentEntity, property)) {
                            outboundRow.remove(property.getColumnName());
                        }
                    });

                    Criteria criteria = Criteria.where(dataAccessStrategy().toSql(idColumn)).is(id);

                    if (tuple.getT3().isPresent()) {
                        criteria = criteria.and(tuple.getT3().get());
                    }

                    return Tuples.of(Update.from((Map) outboundRow), criteria, Optional.of(outboundRow));
                });
    }

    @Override
    protected Mono<R> fetch(R2dbcOperationContext<T, R> operationContext) {
        return fetchBefore(operationContext)
                .flatMap(tuple -> {
                    R2dbcPluginContext<T, R> pluginContext = tuple.getT1();
                    CriteriaDefinition criteria = pluginContext.getCriteria();
                    StatementMapper statementMapper = operationContext.getStatementMapper();
                    SqlIdentifier tableName = operationContext.getTableName();

                    StatementMapper.UpdateSpec selectSpec = operationContext.getStatementMapper()
                            .createUpdate(tableName, Objects.requireNonNull(pluginContext.getUpdate()));

                    if (criteria != null && !criteria.isEmpty()) {
                        selectSpec = selectSpec.withCriteria(criteria);
                    }

                    PreparedOperation<?> operation = statementMapper.getMappedObject(selectSpec);
                    Mono<Long> rowsUpdated = this.databaseClient()
                            .sql(operation)
                            .filter(template().getStatementFilterFunction())
                            .fetch()
                            .rowsUpdated();
                    return Mono.zip(rowsUpdated, Mono.just(pluginContext), Mono.just(tuple.getT2()));
                })
                .flatMap(tuple -> {
                    if (operationContext.getEntity() != null && tuple.getT3().isPresent()) {
                        R2dbcPluginContext<T, R> pluginContext = tuple.getT2();
                        return Mono.just(tuple.getT1())
                                .handle(updateHandler(pluginContext.getEntity(), R2dbcMappingKit.getPersistentEntity(pluginContext.getEntityType())))
                                .then(template().maybeCallAfterSave(pluginContext.getEntity(), tuple.getT3().get(), operationContext.getTableName()))
                                .map(it -> Tuples.of(it, pluginContext));

                    }
                    return Mono.just(tuple.getT1())
                            .zipWith(Mono.just(tuple.getT2()));
                })
                .flatMap(tuple -> fetchAfter(operationContext, tuple));
    }


    private Mono<R> fetchAfter(R2dbcOperationContext<T, R> operationContext, Tuple2<?, R2dbcPluginContext<T, R>> resultTuple) {
        R result = ReflectionKit.cast(resultTuple.getT1());
        return pluginExecutor().runAfter(resultTuple.getT2().withResult(this, result))
                .mapNotNull(R2dbcPluginContext::getResult)
                .doOnSuccess(operationContext::withResult);
    }


    private BiConsumer<Long, SynchronousSink<Object>> updateHandler(T entity, RelationalPersistentEntity<T> persistentEntity) {
        return (rowsUpdated, sink) -> {
            if (rowsUpdated != 0) {
                return;
            }
            if (persistentEntity.hasVersionProperty()) {
                sink.error(new OptimisticLockingFailureException(formatOptimisticLockingExceptionMessage(entity, persistentEntity)));
            } else {
                sink.error(new TransientDataAccessResourceException(formatTransientEntityExceptionMessage(entity, persistentEntity)));
            }
        };
    }

    private <E> Criteria createMatchingVersionCriteria(E entity, RelationalPersistentEntity<E> persistentEntity) {

        PersistentPropertyAccessor<?> propertyAccessor = persistentEntity.getPropertyAccessor(entity);

        Optional<RelationalPersistentProperty> versionPropertyOptional = Optional.ofNullable(persistentEntity.getVersionProperty());

        return versionPropertyOptional.map(versionProperty -> {
            Object version = propertyAccessor.getProperty(versionProperty);
            Criteria.CriteriaStep versionColumn = Criteria.where(template().getDataAccessStrategy().toSql(versionProperty.getColumnName()));
            if (version == null) {
                return versionColumn.isNull();
            } else {
                return versionColumn.is(version);
            }
        }).orElse(Criteria.empty());

    }

    private <E> E incrementVersion(RelationalPersistentEntity<E> persistentEntity, E entity) {

        PersistentPropertyAccessor<?> propertyAccessor = persistentEntity.getPropertyAccessor(entity);

        Optional<RelationalPersistentProperty> versionPropertyOptional = Optional.ofNullable(persistentEntity.getVersionProperty());

        versionPropertyOptional.ifPresent(versionProperty -> {
            ConversionService conversionService = this.converter().getConversionService();
            Optional<Object> currentVersionValue = Optional.ofNullable(propertyAccessor.getProperty(versionProperty));

            long newVersionValue = currentVersionValue.map(it -> conversionService.convert(it, Long.class)).map(it -> it + 1).orElse(1L);

            propertyAccessor.setProperty(versionProperty, conversionService.convert(newVersionValue, versionProperty.getType()));
        });
        return (E) propertyAccessor.getBean();
    }

    private <E> String formatOptimisticLockingExceptionMessage(E entity, RelationalPersistentEntity<E> persistentEntity) {

        return String.format("Failed to update table [%s]; Version does not match for row with Id [%s]", persistentEntity.getQualifiedTableName(), persistentEntity.getIdentifierAccessor(entity).getIdentifier());
    }

    private <E> String formatTransientEntityExceptionMessage(E entity, RelationalPersistentEntity<E> persistentEntity) {

        return String.format("Failed to update table [%s]; Row with Id [%s] does not exist", persistentEntity.getQualifiedTableName(), persistentEntity.getIdentifierAccessor(entity).getIdentifier());
    }


    public static final class R2dbcUpdateExecutorBuilder<T, R> extends R2dbcOperationExecutor.R2dbcExecutorBuilder<T, R, R2dbcUpdateExecutor<T, R>, R2dbcUpdateExecutorBuilder<T, R>> {

        private Supplier<Update> updateSupplier;

        public R2dbcUpdateExecutorBuilder<T, R> updateSupplier(Supplier<Update> updateSupplier) {
            this.updateSupplier = updateSupplier;
            return this;
        }

        public R2dbcUpdateExecutor<T, R> buildExecutor() {
            return new R2dbcUpdateExecutor<>(operationContext, queryHandler, updateSupplier);
        }

        @Override
        protected R2dbcUpdateExecutorBuilder<T, R> self() {
            return this;
        }

    }

}
