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

import com.jxpanda.r2dbc.spring.data.config.R2dbcConfigProperties;
import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcDeleteOperation;
import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcInsertOperation;
import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcSelectOperation;
import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcUpdateOperation;
import com.jxpanda.r2dbc.spring.data.core.operation.support.*;
import org.springframework.dao.DataAccessException;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public class ReactiveEntityTemplate extends R2dbcEntityTemplate {


    private final SpelAwareProxyProjectionFactory projectionFactory;

    private final R2dbcConfigProperties r2dbcConfigProperties;

    private final R2dbcSQLExecutor sqlExecutor;


    public ReactiveEntityTemplate(DatabaseClient databaseClient, R2dbcDialect dialect, R2dbcConverter converter, R2dbcConfigProperties r2dbcConfigProperties) {
        super(databaseClient, dialect, converter);
        this.projectionFactory = new SpelAwareProxyProjectionFactory();
        this.r2dbcConfigProperties = r2dbcConfigProperties;
        this.sqlExecutor = new R2dbcSQLExecutor(this, dialect, converter);
    }

    public R2dbcSQLExecutor getSqlExecutor() {
        return sqlExecutor;
    }

    public R2dbcConfigProperties getR2dbcConfigProperties() {
        return r2dbcConfigProperties;
    }

    @Override
    public <T> Mono<T> maybeCallBeforeConvert(T object, SqlIdentifier table) {
        return super.maybeCallBeforeConvert(object, table);
    }

    @Override
    public <T> Mono<T> maybeCallBeforeSave(T object, OutboundRow row, SqlIdentifier table) {
        return super.maybeCallBeforeSave(object, row, table);
    }

    @Override
    public <T> Mono<T> maybeCallAfterSave(T object, OutboundRow row, SqlIdentifier table) {
        return super.maybeCallAfterSave(object, row, table);
    }

    @Override
    public <T> Mono<T> maybeCallAfterConvert(T object, SqlIdentifier table) {
        return super.maybeCallAfterConvert(object, table);
    }


    public SpelAwareProxyProjectionFactory getProjectionFactory() {
        return projectionFactory;
    }


    // -------------------------------------------------------------------------
    // Methods dealing with org.springframework.data.r2dbc.core.FluentR2dbcOperations
    // -------------------------------------------------------------------------


    @Override
    public <T> R2dbcSelectOperation.R2dbcSelect<T> select(Class<T> domainType) {
        return new R2dbcSelectOperationSupport(this)
                .select(domainType);
    }

    @Override
    public <T> Mono<T> selectOne(Query query, Class<T> entityClass) throws DataAccessException {
        return select(entityClass)
                .matching(query)
                .one();
    }

    @Override
    public <T> Flux<T> select(Query query, Class<T> entityClass) throws DataAccessException {
        return select(entityClass)
                .matching(query)
                .all();
    }

    @Override
    public <T> R2dbcInsertOperation.R2dbcInsert<T> insert(Class<T> domainType) {
        return new R2dbcInsertOperationSupport(this).insert(domainType);
    }

    @Override
    public <T> Mono<T> insert(T entity) throws DataAccessException {
        return insert(this.sqlExecutor.getRequiredEntity(entity).getType()).using(entity);
    }

    public <T> Flux<T> batchInsert(Collection<T> entityList, Class<T> domainType) {
        return insert(domainType).batchInsert(entityList);
    }

    @Override
    public R2dbcUpdateOperation.R2dbcUpdate update(Class<?> domainType) {
        return new R2dbcUpdateOperationSupport(this).update(domainType);
    }

    @Override
    public <T> Mono<T> update(T entity) throws DataAccessException {
        return update(entity.getClass()).using(entity);
    }

    @Override
    public Mono<Long> update(Query query, Update update, Class<?> entityClass) throws DataAccessException {
        return update(entityClass).matching(query).apply(update);
    }

    @Override
    public R2dbcDeleteOperation.R2dbcDelete delete(Class<?> domainType) {
        return new R2dbcDeleteOperationSupport(this).delete(domainType);
    }

    @Override
    public <T> Mono<T> delete(T entity) throws DataAccessException {
        return delete(this.sqlExecutor.getRequiredEntity(entity).getType()).using(entity).thenReturn(entity);
    }


}
