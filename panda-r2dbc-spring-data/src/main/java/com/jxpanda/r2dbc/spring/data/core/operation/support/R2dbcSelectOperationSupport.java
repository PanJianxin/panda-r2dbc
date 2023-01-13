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
package com.jxpanda.r2dbc.spring.data.core.operation.support;


import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcSelectOperation;
import org.springframework.data.r2dbc.core.ReactiveSelectOperation;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.RowsFetchSpec;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * Implementation of {@link ReactiveSelectOperation}.
 *
 * @author Mark Paluch
 * @since 1.1
 */
public final class R2dbcSelectOperationSupport extends R2dbcOperationSupport implements ReactiveSelectOperation {

    public R2dbcSelectOperationSupport(ReactiveEntityTemplate template) {
        super(template);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation#select(java.lang.Class)
     */
    @NonNull
    @Override
    public <T> R2dbcSelectOperation.R2dbcSelect<T> select(@NonNull Class<T> domainType) {

        Assert.notNull(domainType, "DomainType must not be null");

        return new R2dbcSelectSupport<>(this.template, domainType, domainType, Query.empty(), null);
    }


    private final static class R2dbcSelectSupport<T, R> extends R2dbcSupport<T, R> implements R2dbcSelectOperation.R2dbcSelect<R> {

        private R2dbcSelectSupport(ReactiveEntityTemplate template, Class<T> domainType, Class<R> returnType, Query query,
                                   @Nullable SqlIdentifier tableName) {
            super(template, domainType, returnType, query, tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.SelectWithTable#from(java.lang.String)
         */
        @NonNull
        @Override
        public SelectWithProjection<R> from(@NonNull SqlIdentifier tableName) {

            Assert.notNull(tableName, "Table name must not be null");

            return new R2dbcSelectSupport<>(getTemplate(), getDomainType(), getReturnType(), getQuery(), tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.SelectWithProjection#as(java.lang.Class)
         */
        @NonNull
        @Override
        public <E> SelectWithQuery<E> as(@NonNull Class<E> returnType) {

            Assert.notNull(returnType, "ReturnType must not be null");

            return new R2dbcSelectSupport<>(getTemplate(), getReturnType(), returnType, getQuery(), getTableName());
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.SelectWithQuery#matching(org.springframework.data.r2dbc.query.Query)
         */
        @NonNull
        @Override
        public TerminatingSelect<R> matching(@NonNull Query query) {

            Assert.notNull(query, "Query must not be null");

            return new R2dbcSelectSupport<>(getTemplate(), getDomainType(), getReturnType(), query, getTableName());
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.TerminatingSelect#count()
         */
        @NonNull
        @Override
        public Mono<Long> count() {
            return getExecutor().doCount(getQuery(), getDomainType(), getTableName());
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.TerminatingSelect#exists()
         */
        @NonNull
        @Override
        public Mono<Boolean> exists() {
            return getExecutor().doExists(getQuery(), getDomainType(), getTableName());
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.TerminatingSelect#first()
         */
        @NonNull
        @Override
        public Mono<R> first() {
            return selectMono(getQuery().limit(1), getDomainType(), getTableName(), getReturnType(), RowsFetchSpec::first);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.TerminatingSelect#one()
         */
        @NonNull
        @Override
        public Mono<R> one() {
            return selectMono(getQuery().limit(2), getDomainType(), getTableName(), getReturnType(), RowsFetchSpec::one);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.TerminatingSelect#all()
         */
        @NonNull
        @Override
        public Flux<R> all() {
            return selectFlux(getQuery(), getDomainType(), getTableName(), getReturnType(), RowsFetchSpec::all);
        }


        private Mono<R> selectMono(Query query, Class<T> entityClass, SqlIdentifier tableName,
                                   Class<R> returnType, Function<RowsFetchSpec<R>, Mono<R>> resultHandler) {
            return resultHandler.apply(getExecutor().doSelect(query, entityClass, tableName, returnType))
                    .flatMap(it -> getTemplate().maybeCallAfterConvert(it, tableName));
        }

        private Flux<R> selectFlux(Query query, Class<T> entityClass, SqlIdentifier tableName,
                                   Class<R> returnType, Function<RowsFetchSpec<R>, Flux<R>> resultHandler) {
            return resultHandler.apply(getExecutor().doSelect(query, entityClass, tableName, returnType))
                    .flatMap(it -> getTemplate().maybeCallAfterConvert(it, tableName));
        }



    }
}
