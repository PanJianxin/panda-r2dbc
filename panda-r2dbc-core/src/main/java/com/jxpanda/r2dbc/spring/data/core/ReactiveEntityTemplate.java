/*
 * Copyright 2020-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jxpanda.r2dbc.spring.data.core;

import com.jxpanda.r2dbc.spring.data.core.enhance.key.IdGenerator;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcPluginExecutor;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.page.Pagination;
import com.jxpanda.r2dbc.spring.data.core.kit.R2dbcMappingKit;
import com.jxpanda.r2dbc.spring.data.core.operation.*;
import com.jxpanda.r2dbc.spring.data.core.operation.support.*;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import jakarta.annotation.Resource;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mapping.callback.ReactiveEntityCallbacks;
import org.springframework.data.projection.EntityProjection;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.r2dbc.convert.EntityRowMapper;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.DefaultReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.r2dbc.mapping.event.AfterConvertCallback;
import org.springframework.data.r2dbc.mapping.event.AfterSaveCallback;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import org.springframework.data.r2dbc.mapping.event.BeforeSaveCallback;
import org.springframework.data.relational.core.conversion.AbstractRelationalConverter;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.relational.domain.RowDocument;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.r2dbc.core.RowsFetchSpec;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

@SuppressWarnings({"unused", "UnusedReturnValue", "deprecation", "unchecked"})
public class ReactiveEntityTemplate implements R2dbcEntityOperations {


    @Getter
    private final DatabaseClient databaseClient;

    @Getter
    private final ReactiveDataAccessStrategy dataAccessStrategy;

    @Getter
    private final R2dbcConverter converter;

    @Getter
    private final R2dbcDialect dialect;

    @Getter(value = AccessLevel.PUBLIC)
    private final SpelAwareProxyProjectionFactory projectionFactory;

    @Getter(value = AccessLevel.PUBLIC)
    private final StatementMapper statementMapper;

    @Nullable
    @Getter(value = AccessLevel.PACKAGE)
    private ReactiveEntityCallbacks entityCallbacks;

    @Resource
    @Getter(value = AccessLevel.PUBLIC)
    private IdGenerator<?> idGenerator;

    @Resource
    @Getter(value = AccessLevel.PUBLIC)
    private R2dbcTransactionManager r2dbcTransactionManager;

    @Resource
    @Getter(value = AccessLevel.PUBLIC)
    private TransactionalOperator transactionalOperator;

    @Resource
    @Getter(value = AccessLevel.PUBLIC)
    private R2dbcPluginExecutor pluginExecutor;


    public ReactiveEntityTemplate(DatabaseClient databaseClient, R2dbcDialect dialect, R2dbcConverter converter) {
        this.databaseClient = databaseClient;
        this.dataAccessStrategy = new DefaultReactiveDataAccessStrategy(dialect, converter);
        this.projectionFactory = new SpelAwareProxyProjectionFactory();
        this.dialect = dialect;
        this.converter = converter;
        this.statementMapper = new R2dbcStatementMapper(dialect, this.converter);
    }


    // -------------------------------------------------------------------------
    // callbacks
    // -------------------------------------------------------------------------

    public <T> Mono<T> maybeCallBeforeConvert(T object, SqlIdentifier table) {
        if (entityCallbacks != null) {
            return entityCallbacks.callback(BeforeConvertCallback.class, object, table);
        }
        return Mono.just(object);
    }

    public <T> Mono<T> maybeCallAfterConvert(T object, SqlIdentifier table) {
        if (entityCallbacks != null) {
            return entityCallbacks.callback(AfterConvertCallback.class, object, table);
        }
        return Mono.just(object);
    }

    public <T> Mono<T> maybeCallBeforeSave(T object, OutboundRow row, SqlIdentifier table) {
        if (entityCallbacks != null) {
            return entityCallbacks.callback(BeforeSaveCallback.class, object, row, table);
        }
        return Mono.just(object);
    }

    public <T> Mono<T> maybeCallAfterSave(T object, OutboundRow row, SqlIdentifier table) {
        if (entityCallbacks != null) {
            return entityCallbacks.callback(AfterSaveCallback.class, object, table);
        }
        return Mono.just(object);
    }


    // -------------------------------------------------------------------------
    // Methods dealing with org.springframework.data.r2dbc.core.FluentR2dbcOperations
    // -------------------------------------------------------------------------

    public <T> R2dbcSelectOperation.R2dbcSelect<T> select(Class<T> domainType) {
        return new R2dbcSelectOperationSupport(this)
                .select(domainType);
    }

    public <T> R2dbcInsertOperation.R2dbcInsert<T> insert(Class<T> domainType) {
        return new R2dbcInsertOperationSupport(this)
                .insert(domainType);
    }

    @Override
    public R2dbcUpdateOperation.R2dbcUpdate<?> update(Class<?> domainType) {
        return new R2dbcUpdateOperationSupport(this)
                .update(domainType);
    }

    public <T> R2dbcSaveOperation.R2dbcSave<T> save(Class<T> domainType) {
        return new R2dbcSaveOperationSupport(this)
                .save(domainType);
    }

    @Override
    public R2dbcDeleteOperation.R2dbcDelete<?> delete(Class<?> domainType) {
        return new R2dbcDeleteOperationSupport(this)
                .delete(domainType);
    }

    public <T> R2dbcDestroyOperation.R2dbcDestroy<T> destroy(Class<T> domainType) {
        return new R2dbcDestroyOperationSupport(this)
                .destroy(domainType);
    }


    // -------------------------------------------------------------------------
    // Methods dealing with org.springframework.data.r2dbc.query.Query
    // -------------------------------------------------------------------------

    public <T, ID> Mono<T> selectById(ID id, Class<T> entityClass) throws DataAccessException {
        return select(entityClass)
                .byId(id);
    }

    public <T> Mono<T> selectOne(Query query, Class<T> entityClass) throws DataAccessException {
        return select(entityClass)
                .matching(query)
                .one();
    }

    @Override
    public Mono<Long> count(Query query, Class<?> entityClass) throws DataAccessException {
        return select(entityClass)
                .matching(query)
                .count();
    }

    @Override
    public Mono<Boolean> exists(Query query, Class<?> entityClass) throws DataAccessException {
        return select(entityClass)
                .matching(query)
                .exists();
    }

    public <T> Flux<T> select(Query query, Class<T> entityClass) throws DataAccessException {
        return select(entityClass)
                .matching(query)
                .all();
    }

    public <T, ID> Flux<T> selectByIds(Collection<ID> ids, Class<T> entityClass) throws DataAccessException {
        return select(entityClass)
                .byIds(ids);
    }

    public <T> Mono<Pagination<T>> page(Query query, Pagination<T> pagination, Class<T> entityClass) throws DataAccessException {
        return select(entityClass)
                .matching(query)
                .page(pagination.getPageable());
    }

    public <T> Mono<T> insert(T entity) throws DataAccessException {
        return insert(R2dbcMappingKit.getRequiredEntity(entity).getType()).using(entity);
    }

    public <T> Flux<T> insertBatch(Collection<T> entityList, Class<T> domainType) {
        return insert(domainType).batch(entityList);
    }

    public <T> Mono<T> update(T entity) throws DataAccessException {
        return new R2dbcUpdateOperationSupport(this)
                .update(R2dbcMappingKit.getRequiredEntity(entity).getType())
                .using(entity);
    }

    public <T> Flux<T> updateBatch(Collection<T> entityList, Class<T> domainType) throws DataAccessException {
        return new R2dbcUpdateOperationSupport(this)
                .update(domainType)
                .batch(entityList);
    }

    public Mono<Long> update(Query query, Update update, Class<?> entityClass) throws DataAccessException {
        return update(entityClass)
                .matching(query)
                .apply(update);
    }


    public <T> Mono<T> save(T entity) throws DataAccessException {
        return save(R2dbcMappingKit.getRequiredEntity(entity).getType()).using(entity);
    }

    public <T> Flux<T> saveBatch(Collection<T> entityList, Class<T> domainType) {
        return save(domainType).batch(entityList);
    }


    public <T> Mono<T> delete(T entity) throws DataAccessException {
        return new R2dbcDeleteOperationSupport(this)
                .delete(R2dbcMappingKit.getRequiredEntity(entity).getType())
                .using(entity)
                .thenReturn(entity);
    }

    public Mono<Long> delete(Query query, Class<?> entityClass) throws DataAccessException {
        return delete(entityClass)
                .matching(query)
                .all();
    }

    public <ID> Mono<Boolean> deleteById(ID id, Class<?> entityClass) {
        return delete(entityClass)
                .byId(id);
    }

    public <ID> Mono<Long> deleteByIds(Collection<ID> ids, Class<?> entityClass) {
        return delete(entityClass)
                .byIds(ids);
    }

    public <T> Mono<T> destroy(T entity) throws DataAccessException {
        return destroy(R2dbcMappingKit.getRequiredEntity(entity).getType())
                .using(entity)
                .thenReturn(entity);
    }

    public Mono<Long> destroy(Query query, Class<?> entityClass) throws DataAccessException {
        return destroy(entityClass)
                .matching(query)
                .all();
    }

    public <ID> Mono<Boolean> destroyById(ID id, Class<?> entityClass) {
        return destroy(entityClass)
                .byId(id);
    }

    public <ID> Mono<Long> destroyByIds(Collection<ID> ids, Class<?> entityClass) {
        return destroy(entityClass)
                .byIds(ids);
    }

    // -------------------------------------------------------------------------
    // Methods dealing with org.springframework.r2dbc.core.PreparedOperation
    // -------------------------------------------------------------------------

    public <E> RowsFetchSpec<E> query(PreparedOperation<?> operation, Class<E> entityClass) {

        Assert.notNull(operation, "PreparedOperation must not be null");
        Assert.notNull(entityClass, "Entity class must not be null");
        return query(operation, entityClass, entityClass);
    }

    @Override
    public <T> RowsFetchSpec<T> query(PreparedOperation<?> operation, Class<?> entityClass, Class<T> resultType) throws DataAccessException {
        Assert.notNull(operation, "PreparedOperation must not be null");
        Assert.notNull(entityClass, "Entity class must not be null");

        return new EntityCallbackAdapter<>(getRowsFetchSpec(getDatabaseClient().sql(operation), entityClass, resultType),
                R2dbcMappingKit.getTableNameOrEmpty(entityClass));
    }

    @Override
    public <T> RowsFetchSpec<T> query(PreparedOperation<?> operation, Function<Row, T> rowMapper) throws DataAccessException {
        return R2dbcEntityOperations.super.query(operation, rowMapper);
    }

    public <E> RowsFetchSpec<E> query(PreparedOperation<?> operation, BiFunction<Row, RowMetadata, E> rowMapper) {

        Assert.notNull(operation, "PreparedOperation must not be null");
        Assert.notNull(rowMapper, "Row mapper must not be null");

        return new EntityCallbackAdapter<>(getDatabaseClient().sql(operation).map(rowMapper), SqlIdentifier.EMPTY);
    }

    @Override
    public <T> RowsFetchSpec<T> query(PreparedOperation<?> operation, Class<?> entityClass, Function<Row, T> rowMapper) throws DataAccessException {
        return R2dbcEntityOperations.super.query(operation, entityClass, rowMapper);
    }

    public <E> RowsFetchSpec<E> query(PreparedOperation<?> operation, Class<?> entityClass,
                                      BiFunction<Row, RowMetadata, E> rowMapper) {

        Assert.notNull(operation, "PreparedOperation must not be null");
        Assert.notNull(entityClass, "Entity class must not be null");
        Assert.notNull(rowMapper, "Row mapper must not be null");

        return new EntityCallbackAdapter<>(getDatabaseClient().sql(operation).map(rowMapper), R2dbcMappingKit.getTableNameOrEmpty(entityClass));
    }


    private <E> BiFunction<Row, RowMetadata, E> getRowMapper(Class<E> typeToRead) {
        return new EntityRowMapper<>(typeToRead, this.getConverter());
    }

    @Override
    public <T> RowsFetchSpec<T> getRowsFetchSpec(DatabaseClient.GenericExecuteSpec executeSpec, Class<?> entityType, Class<T> resultType) {

        boolean simpleType = getConverter().isSimpleType(resultType);

        BiFunction<Row, RowMetadata, T> rowMapper;

        // Bridge-code: Consider Converter<Row, T> until we have fully migrated to RowDocument
        if (converter instanceof AbstractRelationalConverter relationalConverter
            && relationalConverter.getConversions().hasCustomReadTarget(Row.class, resultType)) {

            ConversionService conversionService = relationalConverter.getConversionService();
            rowMapper = (row, rowMetadata) -> (T) conversionService.convert(row, resultType);
        } else if (simpleType) {
            rowMapper = getRowMapper(resultType);
        } else {

            EntityProjection<T, ?> projection = getConverter().introspectProjection(resultType, entityType);
            Class<T> typeToRead = projection.isProjection() ? resultType
                    : resultType.isInterface() ? (Class<T>) entityType : resultType;

            rowMapper = (row, rowMetadata) -> {

                RowDocument document = getDataAccessStrategy().toRowDocument(typeToRead, row, rowMetadata.getColumnMetadatas());
                return getConverter().project(projection, document);
            };
        }

        // avoid top-level null values if the read type is a simple one (e.g. SELECT MAX(age) via Integer.class)
        if (simpleType) {
            return new UnwrapOptionalFetchSpecAdapter<>(
                    executeSpec.map((row, metadata) -> Optional.ofNullable(rowMapper.apply(row, metadata))));
        }

        return executeSpec.map(rowMapper);

    }

    private record UnwrapOptionalFetchSpecAdapter<T>(
            RowsFetchSpec<Optional<T>> delegate) implements RowsFetchSpec<T> {

        @NonNull
        @Override
        public Mono<T> one() {
            return delegate.one().handle((optional, sink) -> optional.ifPresent(sink::next));
        }

        @NonNull
        @Override
        public Mono<T> first() {
            return delegate.first().handle((optional, sink) -> optional.ifPresent(sink::next));
        }

        @NonNull
        @Override
        public Flux<T> all() {
            return delegate.all().handle((optional, sink) -> optional.ifPresent(sink::next));
        }
    }

    /**
     * {@link RowsFetchSpec} adapter applying {@link ReactiveEntityTemplate#maybeCallAfterConvert(Object, SqlIdentifier)} to each emitted
     * object.
     *
     * @param <T>
     */
    private class EntityCallbackAdapter<T> implements RowsFetchSpec<T> {

        private final RowsFetchSpec<T> delegate;
        private final SqlIdentifier tableName;

        private EntityCallbackAdapter(RowsFetchSpec<T> delegate, SqlIdentifier tableName) {
            this.delegate = delegate;
            this.tableName = tableName;
        }

        @Override
        public Mono<T> one() {
            return delegate.one().flatMap(it -> maybeCallAfterConvert(it, tableName));
        }

        @Override
        public Mono<T> first() {
            return delegate.first().flatMap(it -> maybeCallAfterConvert(it, tableName));
        }

        @Override
        public Flux<T> all() {
            return delegate.all().concatMap(it -> maybeCallAfterConvert(it, tableName));
        }
    }


}
