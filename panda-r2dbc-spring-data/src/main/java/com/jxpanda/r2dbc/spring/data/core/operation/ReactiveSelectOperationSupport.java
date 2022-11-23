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
package com.jxpanda.r2dbc.spring.data.core.operation;


import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.extension.annotation.TableEntity;
import org.springframework.data.r2dbc.core.ReactiveSelectOperation;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Expressions;
import org.springframework.data.relational.core.sql.Functions;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.r2dbc.core.RowsFetchSpec;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Function;

/**
 * Implementation of {@link ReactiveSelectOperation}.
 *
 * @author Mark Paluch
 * @since 1.1
 */
public final class ReactiveSelectOperationSupport extends ReactiveOperationSupport implements ReactiveSelectOperation {

    public ReactiveSelectOperationSupport(ReactiveEntityTemplate template) {
        super(template);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation#select(java.lang.Class)
     */
    @Nonnull
    @Override
    public <T> ReactiveSelect<T> select(@Nonnull Class<T> domainType) {

        Assert.notNull(domainType, "DomainType must not be null");

        return new ReactiveSelectSupport<>(this.template, domainType, domainType, Query.empty(), null);
    }


    private final static class ReactiveSelectSupport<T, R> extends ReactiveOperationSupport.ReactiveSupport<T, R> implements ReactiveSelect<R> {

        private ReactiveSelectSupport(ReactiveEntityTemplate template, Class<T> domainType, Class<R> returnType, Query query,
                                      @Nullable SqlIdentifier tableName) {
            super(template, domainType, returnType, query, tableName);
        }

//        private ReactiveSelectSupport(DatabaseClient databaseClient, Class<T> domainType, Class<R> returnType, Query query, SqlIdentifier tableName) {
//            super(databaseClient, domainType, returnType, query, tableName);
//        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.SelectWithTable#from(java.lang.String)
         */
        @Nonnull
        @Override
        public SelectWithProjection<R> from(@Nonnull SqlIdentifier tableName) {

            Assert.notNull(tableName, "Table name must not be null");

            return new ReactiveSelectSupport<>(getTemplate(), getDomainType(), getReturnType(), getQuery(), tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.SelectWithProjection#as(java.lang.Class)
         */
        @Nonnull
        @Override
        public <E> SelectWithQuery<E> as(@Nonnull Class<E> returnType) {

            Assert.notNull(returnType, "ReturnType must not be null");

            return new ReactiveSelectSupport<>(getTemplate(), getReturnType(), returnType, getQuery(), getTableName());
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.SelectWithQuery#matching(org.springframework.data.r2dbc.query.Query)
         */
        @Nonnull
        @Override
        public TerminatingSelect<R> matching(@Nonnull Query query) {

            Assert.notNull(query, "Query must not be null");

            return new ReactiveSelectSupport<>(getTemplate(), getDomainType(), getReturnType(), query, getTableName());
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.TerminatingSelect#count()
         */
        @Nonnull
        @Override
        public Mono<Long> count() {
            return doCount(getQuery(), getDomainType(), getTableName());
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.TerminatingSelect#exists()
         */
        @Nonnull
        @Override
        public Mono<Boolean> exists() {
            return doExists(getQuery(), getDomainType(), getTableName());
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.TerminatingSelect#first()
         */
        @Nonnull
        @Override
        public Mono<R> first() {
            return selectMono(getQuery().limit(1), getDomainType(), getTableName(), getReturnType(), RowsFetchSpec::first);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.TerminatingSelect#one()
         */
        @Nonnull
        @Override
        public Mono<R> one() {
            return selectMono(getQuery().limit(2), getDomainType(), getTableName(), getReturnType(), RowsFetchSpec::one);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.TerminatingSelect#all()
         */
        @Nonnull
        @Override
        public Flux<R> all() {
            return selectFlux(getQuery(), getDomainType(), getTableName(), getReturnType(), RowsFetchSpec::all);
        }


        private Mono<R> selectMono(Query query, Class<T> entityClass, SqlIdentifier tableName,
                                   Class<R> returnType, Function<RowsFetchSpec<R>, Mono<R>> resultHandler) {
            return resultHandler.apply(doSelect(query, entityClass, tableName, returnType))
                    .flatMap(it -> maybeCallAfterConvert(it, tableName));
        }

        private Flux<R> selectFlux(Query query, Class<T> entityClass, SqlIdentifier tableName,
                                   Class<R> returnType, Function<RowsFetchSpec<R>, Flux<R>> resultHandler) {
            return resultHandler.apply(doSelect(query, entityClass, tableName, returnType))
                    .flatMap(it -> maybeCallAfterConvert(it, tableName));
        }

        public Mono<Boolean> doExists(Query query, Class<T> entityClass, SqlIdentifier tableName) {

            RelationalPersistentEntity<T> entity = getRequiredEntity(entityClass);
            StatementMapper statementMapper = getStatementMapper().forType(entityClass);

            SqlIdentifier columnName = entity.hasIdProperty() ? entity.getRequiredIdProperty().getColumnName()
                    : SqlIdentifier.unquoted("*");

            StatementMapper.SelectSpec selectSpec = statementMapper
                    .createSelect(tableName)
                    .withProjection(columnName)
                    .limit(1);

            Optional<CriteriaDefinition> criteria = query.getCriteria();
            if (criteria.isPresent()) {
                selectSpec = criteria.map(selectSpec::withCriteria).orElse(selectSpec);
            }

            PreparedOperation<?> operation = statementMapper.getMappedObject(selectSpec);

            return getDatabaseClient().sql(operation)
                    .map((r, md) -> r)
                    .first()
                    .hasElement();
        }

        public Mono<Long> doCount(Query query, Class<T> entityClass, SqlIdentifier tableName) {

            RelationalPersistentEntity<T> entity = getRequiredEntity(entityClass);
            StatementMapper statementMapper = getStatementMapper().forType(entityClass);

            StatementMapper.SelectSpec selectSpec = statementMapper
                    .createSelect(tableName)
                    .doWithTable((table, spec) -> {
                        Expression countExpression = entity.hasIdProperty()
                                ? table.column(entity.getRequiredIdProperty().getColumnName())
                                : Expressions.asterisk();
                        return spec.withProjection(Functions.count(countExpression));
                    });

            Optional<CriteriaDefinition> criteria = query.getCriteria();
            if (criteria.isPresent()) {
                selectSpec = criteria.map(selectSpec::withCriteria).orElse(selectSpec);
            }

            PreparedOperation<?> operation = statementMapper.getMappedObject(selectSpec);

            return getDatabaseClient().sql(operation)
                    .map((r, md) -> r.get(0, Long.class))
                    .first()
                    .defaultIfEmpty(0L);
        }

        private RowsFetchSpec<R> doSelect(Query query, Class<T> entityClass, SqlIdentifier tableName, Class<R> returnType) {

            boolean isQueryEntity = false;

            if (entityClass.isAnnotationPresent(TableEntity.class)) {
                isQueryEntity = entityClass.getAnnotation(TableEntity.class).isQuery();
            }

            StatementMapper statementMapper = isQueryEntity ? getStatementMapper() : getStatementMapper().forType(entityClass);

            StatementMapper.SelectSpec selectSpec = statementMapper
                    .createSelect(tableName)
                    .doWithTable((table, spec) -> spec.withProjection(getSelectProjection(table, query, entityClass, returnType)));

            if (query.getLimit() > 0) {
                selectSpec = selectSpec.limit(query.getLimit());
            }

            if (query.getOffset() > 0) {
                selectSpec = selectSpec.offset(query.getOffset());
            }

            if (query.isSorted()) {
                selectSpec = selectSpec.withSort(query.getSort());
            }

            Optional<CriteriaDefinition> criteria = query.getCriteria();
            if (criteria.isPresent()) {
                selectSpec = criteria.map(selectSpec::withCriteria).orElse(selectSpec);
            }

            PreparedOperation<?> operation = statementMapper.getMappedObject(selectSpec);

            return getRowsFetchSpec(getDatabaseClient().sql(operation), entityClass, returnType);
        }

    }
}
