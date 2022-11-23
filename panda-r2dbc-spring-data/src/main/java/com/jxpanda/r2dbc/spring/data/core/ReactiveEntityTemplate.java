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

import com.jxpanda.r2dbc.spring.data.core.operation.ReactiveSelectOperationSupport;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mapping.callback.ReactiveEntityCallbacks;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.DefaultReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.util.ProxyUtils;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

import java.util.Objects;

@SuppressWarnings("unchecked")
public class ReactiveEntityTemplate extends R2dbcEntityTemplate {


    private final SpelAwareProxyProjectionFactory projectionFactory;

    private @Nullable ReactiveEntityCallbacks entityCallbacks;

    public ReactiveEntityTemplate(ConnectionFactory connectionFactory) {
        super(connectionFactory);
        this.projectionFactory = new SpelAwareProxyProjectionFactory();
    }

    public ReactiveEntityTemplate(DatabaseClient databaseClient, R2dbcDialect dialect) {
        super(databaseClient, dialect);
        this.projectionFactory = new SpelAwareProxyProjectionFactory();
    }

    public ReactiveEntityTemplate(DatabaseClient databaseClient, R2dbcDialect dialect, R2dbcConverter converter) {
        super(databaseClient, dialect, converter);
        this.projectionFactory = new SpelAwareProxyProjectionFactory();
    }

    public ReactiveEntityTemplate(DatabaseClient databaseClient, DefaultReactiveDataAccessStrategy strategy) {
        super(databaseClient, strategy);
        this.projectionFactory = new SpelAwareProxyProjectionFactory();
    }

    // -------------------------------------------------------------------------
    // Methods dealing with org.springframework.data.r2dbc.core.FluentR2dbcOperations
    // -------------------------------------------------------------------------


    public SpelAwareProxyProjectionFactory getProjectionFactory() {
        return projectionFactory;
    }

    SqlIdentifier getTableName(Class<?> entityClass) {
        return getRequiredEntity(entityClass).getTableName();
    }

    public MappingContext<? extends RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> getMappingContext() {
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
        return (RelationalPersistentEntity<T>) getRequiredEntity(entityType);
    }


    @Override
    public void setEntityCallbacks(@Nullable ReactiveEntityCallbacks entityCallbacks) {
        this.entityCallbacks = entityCallbacks;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

        if (entityCallbacks == null) {
            setEntityCallbacks(ReactiveEntityCallbacks.create(applicationContext));
        }

        projectionFactory.setBeanFactory(applicationContext);
        projectionFactory.setBeanClassLoader(Objects.requireNonNull(applicationContext.getClassLoader()));
    }

    @Nullable
    public ReactiveEntityCallbacks getEntityCallbacks() {
        return entityCallbacks;
    }


    @Override
    public <T> ReactiveSelect<T> select(Class<T> domainType) {
        return new ReactiveSelectOperationSupport(this).select(domainType);
    }

    @Override
    public <T> Mono<T> selectOne(Query query, Class<T> entityClass) throws DataAccessException {
        return super.selectOne(query, entityClass);
    }
}
