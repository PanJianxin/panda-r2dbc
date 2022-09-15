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

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.ConnectionAccessor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.util.ProxyUtils;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class ReactiveEntityTemplate extends R2dbcEntityTemplate {

    public ReactiveEntityTemplate(ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    public ReactiveEntityTemplate(DatabaseClient databaseClient, R2dbcDialect dialect) {
        super(databaseClient, dialect);
    }

    public ReactiveEntityTemplate(DatabaseClient databaseClient, R2dbcDialect dialect, R2dbcConverter converter) {
        super(databaseClient, dialect, converter);
    }

    public ReactiveEntityTemplate(DatabaseClient databaseClient, ReactiveDataAccessStrategy strategy) {
        super(databaseClient, strategy);
    }

    // -------------------------------------------------------------------------
    // Methods dealing with org.springframework.data.r2dbc.core.FluentR2dbcOperations
    // -------------------------------------------------------------------------



    SqlIdentifier getTableName(Class<?> entityClass) {
        return getRequiredEntity(entityClass).getTableName();
    }

    private MappingContext<? extends RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> getMappingContext(){
        return this.getDataAccessStrategy().getConverter().getMappingContext();
    }

    SqlIdentifier getTableNameOrEmpty(Class<?> entityClass) {

        RelationalPersistentEntity<?> entity = getMappingContext().getPersistentEntity(entityClass);

        return entity != null ? entity.getTableName() : SqlIdentifier.EMPTY;
    }

    private RelationalPersistentEntity<?> getRequiredEntity(Class<?> entityClass) {
        return getMappingContext().getRequiredPersistentEntity(entityClass);
    }

    private <T> RelationalPersistentEntity<T> getRequiredEntity(T entity) {
        Class<?> entityType = ProxyUtils.getUserClass(entity);
        return (RelationalPersistentEntity) getRequiredEntity(entityType);
    }


//    private static ReactiveDataAccessStrategy getDataAccessStrategy(
//            org.springframework.data.r2dbc.core.DatabaseClient databaseClient) {
//
//        Assert.notNull(databaseClient, "DatabaseClient must not be null");
//
//        if (databaseClient instanceof DefaultDatabaseClient) {
//
//            DefaultDatabaseClient client = (DefaultDatabaseClient) databaseClient;
//            return client.getDataAccessStrategy();
//        }
//
//        throw new IllegalStateException("Cannot obtain ReactiveDataAccessStrategy");
//    }

    /**
     * Adapter to adapt our deprecated {@link org.springframework.data.r2dbc.core.DatabaseClient} into Spring R2DBC
     * {@link DatabaseClient}.
     */
    private static class DatabaseClientAdapter implements DatabaseClient {

        private final org.springframework.data.r2dbc.core.DatabaseClient delegate;

        private DatabaseClientAdapter(org.springframework.data.r2dbc.core.DatabaseClient delegate) {

            Assert.notNull(delegate, "DatabaseClient must not be null");

            this.delegate = delegate;
        }

        @Override
        public ConnectionFactory getConnectionFactory() {
            return delegate.getConnectionFactory();
        }

        @Override
        public GenericExecuteSpec sql(String sql) {
            return new DatabaseClientAdapter.GenericExecuteSpecAdapter(delegate.execute(sql));
        }

        @Override
        public GenericExecuteSpec sql(Supplier<String> sqlSupplier) {
            return new DatabaseClientAdapter.GenericExecuteSpecAdapter(delegate.execute(sqlSupplier));
        }

        @Override
        public <T> Mono<T> inConnection(Function<Connection, Mono<T>> action) throws DataAccessException {
            return ((ConnectionAccessor) delegate).inConnection(action);
        }

        @Override
        public <T> Flux<T> inConnectionMany(Function<Connection, Flux<T>> action) throws DataAccessException {
            return ((ConnectionAccessor) delegate).inConnectionMany(action);
        }

        static class GenericExecuteSpecAdapter implements GenericExecuteSpec {

            private final org.springframework.data.r2dbc.core.DatabaseClient.GenericExecuteSpec delegate;

            public GenericExecuteSpecAdapter(org.springframework.data.r2dbc.core.DatabaseClient.GenericExecuteSpec delegate) {
                this.delegate = delegate;
            }

            @Override
            public GenericExecuteSpec bind(int index, Object value) {
                return new DatabaseClientAdapter.GenericExecuteSpecAdapter(delegate.bind(index, value));
            }

            @Override
            public GenericExecuteSpec bindNull(int index, Class<?> type) {
                return new DatabaseClientAdapter.GenericExecuteSpecAdapter(delegate.bindNull(index, type));
            }

            @Override
            public GenericExecuteSpec bind(String name, Object value) {
                return new DatabaseClientAdapter.GenericExecuteSpecAdapter(delegate.bind(name, value));
            }

            @Override
            public GenericExecuteSpec bindNull(String name, Class<?> type) {
                return new DatabaseClientAdapter.GenericExecuteSpecAdapter(delegate.bindNull(name, type));
            }

            @Override
            public GenericExecuteSpec filter(org.springframework.r2dbc.core.StatementFilterFunction filter) {
                return new DatabaseClientAdapter.GenericExecuteSpecAdapter(delegate.filter(filter::filter));
            }

            @Override
            public <R> org.springframework.r2dbc.core.RowsFetchSpec<R> map(BiFunction<Row, RowMetadata, R> mappingFunction) {
                return new DatabaseClientAdapter.RowFetchSpecAdapter<>(delegate.map(mappingFunction));
            }

            @Override
            public org.springframework.r2dbc.core.FetchSpec<Map<String, Object>> fetch() {
                return new DatabaseClientAdapter.FetchSpecAdapter<>(delegate.fetch());
            }

            @Override
            public Mono<Void> then() {
                return delegate.then();
            }
        }

        private static class RowFetchSpecAdapter<T> implements org.springframework.r2dbc.core.RowsFetchSpec<T> {

            private final org.springframework.data.r2dbc.core.RowsFetchSpec<T> delegate;

            RowFetchSpecAdapter(org.springframework.data.r2dbc.core.RowsFetchSpec<T> delegate) {
                this.delegate = delegate;
            }

            @Override
            public Mono<T> one() {
                return delegate.one();
            }

            @Override
            public Mono<T> first() {
                return delegate.first();
            }

            @Override
            public Flux<T> all() {
                return delegate.all();
            }
        }

        private static class FetchSpecAdapter<T> extends DatabaseClientAdapter.RowFetchSpecAdapter<T> implements org.springframework.r2dbc.core.FetchSpec<T> {

            private final org.springframework.data.r2dbc.core.FetchSpec<T> delegate;

            FetchSpecAdapter(org.springframework.data.r2dbc.core.FetchSpec<T> delegate) {
                super(delegate);
                this.delegate = delegate;
            }

            @Override
            public Mono<Integer> rowsUpdated() {
                return delegate.rowsUpdated();
            }
        }
    }

    /**
     * {@link org.springframework.r2dbc.core.RowsFetchSpec} adapter emitting values from {@link Optional} if they exist.
     *
     * @param <T>
     */
    private static class UnwrapOptionalFetchSpecAdapter<T> implements org.springframework.r2dbc.core.RowsFetchSpec<T> {

        private final org.springframework.r2dbc.core.RowsFetchSpec<Optional<T>> delegate;

        private UnwrapOptionalFetchSpecAdapter(org.springframework.r2dbc.core.RowsFetchSpec<Optional<T>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Mono<T> one() {
            return delegate.one().handle((optional, sink) -> optional.ifPresent(sink::next));
        }

        @Override
        public Mono<T> first() {
            return delegate.first().handle((optional, sink) -> optional.ifPresent(sink::next));
        }

        @Override
        public Flux<T> all() {
            return delegate.all().handle((optional, sink) -> optional.ifPresent(sink::next));
        }
    }

    /**
     * {@link org.springframework.r2dbc.core.RowsFetchSpec} adapter applying {@link #maybeCallAfterConvert(Object, SqlIdentifier)} to each emitted
     * object.
     *
     * @param <T>
     */
    private class EntityCallbackAdapter<T> implements org.springframework.r2dbc.core.RowsFetchSpec<T> {

        private final org.springframework.r2dbc.core.RowsFetchSpec<T> delegate;
        private final SqlIdentifier tableName;

        private EntityCallbackAdapter(org.springframework.r2dbc.core.RowsFetchSpec<T> delegate, SqlIdentifier tableName) {
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
            return delegate.all().flatMap(it -> maybeCallAfterConvert(it, tableName));
        }
    }
    
}
