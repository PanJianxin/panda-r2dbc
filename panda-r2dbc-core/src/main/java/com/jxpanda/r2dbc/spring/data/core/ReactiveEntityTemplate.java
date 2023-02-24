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
import com.jxpanda.r2dbc.spring.data.core.kit.R2dbcMappingKit;
import com.jxpanda.r2dbc.spring.data.core.operation.*;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mapping.callback.ReactiveEntityCallbacks;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.r2dbc.convert.EntityRowMapper;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.DefaultReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.r2dbc.mapping.event.AfterConvertCallback;
import org.springframework.data.r2dbc.mapping.event.AfterSaveCallback;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import org.springframework.data.r2dbc.mapping.event.BeforeSaveCallback;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.data.relational.core.sql.SqlIdentifier;
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

@Getter
@SuppressWarnings({"unused", "UnusedReturnValue","deprecation"})
public class ReactiveEntityTemplate {



    private final DatabaseClient databaseClient;

    private final ReactiveDataAccessStrategy dataAccessStrategy;

    private final SpelAwareProxyProjectionFactory projectionFactory;

    private final R2dbcConverter converter;

    private final R2dbcDialect dialect;

    @Autowired
    private IdGenerator<?> idGenerator;

    @Autowired
    private R2dbcTransactionManager r2dbcTransactionManager;

    @Autowired
    private TransactionalOperator transactionalOperator;

    private @Nullable ReactiveEntityCallbacks entityCallbacks;


    public ReactiveEntityTemplate(DatabaseClient databaseClient, R2dbcDialect dialect, R2dbcConverter converter) {
        this.databaseClient = databaseClient;
        this.dataAccessStrategy = new DefaultReactiveDataAccessStrategy(dialect, converter);
        this.projectionFactory = new SpelAwareProxyProjectionFactory();
        this.dialect = dialect;
        this.converter = converter;
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
        return new R2dbcInsertOperationSupport(this).insert(domainType);
    }

    public <T> R2dbcUpdateOperation.R2dbcUpdate<T> update(Class<T> domainType) {
       return   new R2dbcUpdateOperationSupport(this).update(domainType);
    }

    public <T> R2dbcSaveOperation.R2dbcSave<T> save(Class<T> domainType) {
        return new R2dbcSaveOperationSupport(this).save(domainType);
    }

    public <T> R2dbcDeleteOperation.R2dbcDelete<T> delete(Class<T> domainType) {
        return new R2dbcDeleteOperationSupport(this).delete(domainType);
    }

    public <T> R2dbcDestroyOperation.R2dbcDestroy<T> destroy(Class<T> domainType) {
        return new R2dbcDestroyOperationSupport(this).destroy(domainType);
    }

    // -------------------------------------------------------------------------
    // Methods dealing with org.springframework.data.r2dbc.query.Query
    // -------------------------------------------------------------------------

    public <T> Mono<T> selectOne(Query query, Class<T> entityClass) throws DataAccessException {
        return select(entityClass)
                .matching(query)
                .one();
    }

    public <T> Flux<T> select(Query query, Class<T> entityClass) throws DataAccessException {
        return select(entityClass)
                .matching(query)
                .all();
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

    public Mono<Long> update(Query query, Update update, Class<?> entityClass) throws DataAccessException {
        return update(entityClass).matching(query).apply(update);
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


    // -------------------------------------------------------------------------
    // Methods dealing with org.springframework.r2dbc.core.PreparedOperation
    // -------------------------------------------------------------------------

    <E> RowsFetchSpec<E> query(PreparedOperation<?> operation, Class<E> entityClass) {

        Assert.notNull(operation, "PreparedOperation must not be null");
        Assert.notNull(entityClass, "Entity class must not be null");

        return new EntityCallbackAdapter<>(getRowsFetchSpec(getDatabaseClient().sql(operation), entityClass, entityClass),
                R2dbcMappingKit.getTableNameOrEmpty(entityClass));
    }

    <E> RowsFetchSpec<E> query(PreparedOperation<?> operation, BiFunction<Row, RowMetadata, E> rowMapper) {

        Assert.notNull(operation, "PreparedOperation must not be null");
        Assert.notNull(rowMapper, "Row mapper must not be null");

        return new EntityCallbackAdapter<>(getDatabaseClient().sql(operation).map(rowMapper), SqlIdentifier.EMPTY);
    }

    <E> RowsFetchSpec<E> query(PreparedOperation<?> operation, Class<?> entityClass,
                               BiFunction<Row, RowMetadata, E> rowMapper) {

        Assert.notNull(operation, "PreparedOperation must not be null");
        Assert.notNull(entityClass, "Entity class must not be null");
        Assert.notNull(rowMapper, "Row mapper must not be null");

        return new EntityCallbackAdapter<>(getDatabaseClient().sql(operation).map(rowMapper), R2dbcMappingKit.getTableNameOrEmpty(entityClass));
    }


    private <E> BiFunction<Row, RowMetadata, E> getRowMapper(Class<E> typeToRead) {
        return new EntityRowMapper<>(typeToRead, this.getConverter());
    }


     <E, RT> RowsFetchSpec<RT> getRowsFetchSpec(DatabaseClient.GenericExecuteSpec executeSpec, Class<E> entityClass, Class<RT> returnType) {

        boolean simpleType;

        BiFunction<Row, RowMetadata, RT> rowMapper;
        if (returnType.isInterface()) {
            simpleType = this.getConverter().isSimpleType(entityClass);
            rowMapper = getRowMapper(entityClass).andThen(source -> this.getProjectionFactory().createProjection(returnType, source));
        } else {
            simpleType = this.getConverter().isSimpleType(returnType);
            rowMapper = getRowMapper(returnType);
        }

        // avoid top-level null values if the read type is a simple one (e.g. SELECT MAX(age) via Integer.class)
        if (simpleType) {
            return new UnwrapOptionalFetchSpecAdapter<>(executeSpec.map((row, metadata) -> Optional.ofNullable(rowMapper.apply(row, metadata))));
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
