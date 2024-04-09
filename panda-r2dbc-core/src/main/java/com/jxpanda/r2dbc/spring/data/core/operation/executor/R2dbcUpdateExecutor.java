package com.jxpanda.r2dbc.spring.data.core.operation.executor;

import com.jxpanda.r2dbc.spring.data.core.kit.R2dbcMappingKit;
import org.reactivestreams.Publisher;
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

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings({"deprecation", "unchecked", "rawtypes"})
public class R2dbcUpdateExecutor<T, R> extends R2dbcOperationExecutor.WriteExecutor<T, R> {

    private final Supplier<Update> updateSupplier;

    private R2dbcUpdateExecutor(R2dbcOperationParameter<T, R> operationParameter,
                               Function<R2dbcOperationParameter<T, R>, Query> queryHandler,
                               Supplier<Update> updateSupplier
    ) {
        super(operationParameter, queryHandler);
        this.updateSupplier = updateSupplier;
    }

    public static <T, R> R2dbcUpdateExecutorBuilder<T, R> builder() {
        return new R2dbcUpdateExecutorBuilder<>();
    }


    @Override
    protected Mono<R> fetch(R2dbcOperationParameter<T, R> parameter) {
        return doFetch(parameter, updateSupplier, parameter.getQuery().getCriteria().orElse(Criteria.empty()))
                .cast(parameter.getReturnType());
    }

    @Override
    protected Mono<R> fetch(T domainEntity, R2dbcOperationParameter<T, R> parameter) {
        RelationalPersistentEntity<T> persistentEntity = parameter.getRelationalPersistentEntity();
        SqlIdentifier tableName = parameter.getTableName();
        Mono<T> mono = template().maybeCallBeforeConvert(domainEntity, tableName).flatMap(onBeforeConvert -> {
            T entityToUse;
            Criteria matchingVersionCriteria;

            if (persistentEntity.hasVersionProperty()) {
                matchingVersionCriteria = createMatchingVersionCriteria(onBeforeConvert, persistentEntity);
                entityToUse = incrementVersion(persistentEntity, onBeforeConvert);
            } else {
                entityToUse = onBeforeConvert;
                matchingVersionCriteria = null;
            }

            OutboundRow outboundRow = getOutboundRow(entityToUse);

            return template().maybeCallBeforeSave(entityToUse, outboundRow, tableName).flatMap(onBeforeSave -> {

                SqlIdentifier idColumn = persistentEntity.getRequiredIdProperty().getColumnName();
                Parameter id = outboundRow.remove(idColumn);

                persistentEntity.forEach(property -> {
                    if (property.isInsertOnly() || !R2dbcMappingKit.isPropertyEffective(entityToUse, persistentEntity, property)) {
                        outboundRow.remove(property.getColumnName());
                    }
                });

                Criteria criteria = Criteria.where(dataAccessStrategy().toSql(idColumn)).is(id);

                if (matchingVersionCriteria != null) {
                    criteria = criteria.and(matchingVersionCriteria);
                }


                Supplier<Update> updateSupplier = () -> Update.from((Map) outboundRow);

                return doFetch(parameter, updateSupplier, criteria)
                        .handle(updateHandler(onBeforeSave, persistentEntity))
                        .then(template().maybeCallAfterSave(onBeforeSave, outboundRow, tableName));
            });
        });
        return mono.cast(parameter.getReturnType());
    }


    private Mono<Long> doFetch(R2dbcOperationParameter<T, R> parameter, Supplier<Update> updateSupplier, CriteriaDefinition criteria) {
        StatementMapper statementMapper = parameter.getStatementMapper();
        SqlIdentifier tableName = parameter.getTableName();

        StatementMapper.UpdateSpec selectSpec = parameter.getStatementMapper().createUpdate(tableName, updateSupplier.get());

        if (criteria != null && !criteria.isEmpty()) {
            selectSpec = selectSpec.withCriteria(criteria);
        }

        PreparedOperation<?> operation = statementMapper.getMappedObject(selectSpec);
        return this.databaseClient().sql(operation).fetch().rowsUpdated();
    }

    private BiConsumer<Long, SynchronousSink<Object>> updateHandler(T domainEntity, RelationalPersistentEntity<T> persistentEntity) {
        return (rowsUpdated, sink) -> {
            if (rowsUpdated != 0) {
                return;
            }
            if (persistentEntity.hasVersionProperty()) {
                sink.error(new OptimisticLockingFailureException(formatOptimisticLockingExceptionMessage(domainEntity, persistentEntity)));
            } else {
                sink.error(new TransientDataAccessResourceException(formatTransientEntityExceptionMessage(domainEntity, persistentEntity)));
            }
        };
    }

//

//    /**
//     * 批量插入数据
//     * 暂时使用循环来做
//     * 后期考虑通过批量插入语句来做
//     */
//    private <E> Flux<E> doUpdateBatch(Collection<E> entityList, SqlIdentifier tableName) {
//        // 这里要管理事务，这个函数不是public的，不能使用@Transactional注解来开启事务
//        // 需要主动管理
//        return Mono.just(entityList)
//                .filter(list -> !ObjectUtils.isEmpty(list))
//                .flatMapMany(Flux::fromIterable)
//                .flatMap(entity -> doUpdate(entity, tableName))
//                .switchIfEmpty(Flux.empty())
//                .as(this.transactionalOperator()::transactional);
//    }

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
            return new R2dbcUpdateExecutor<>(operationParameter, queryHandler, updateSupplier);
        }

        @Override
        protected R2dbcUpdateExecutorBuilder<T, R> self() {
            return this;
        }

    }

}
