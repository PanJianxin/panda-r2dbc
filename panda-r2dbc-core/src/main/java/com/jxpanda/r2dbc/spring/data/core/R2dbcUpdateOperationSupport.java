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

import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcUpdateOperation;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link R2dbcUpdateOperation}.
 *
 * @author Mark Paluch
 * @since 1.1
 */
public final class R2dbcUpdateOperationSupport extends R2dbcOperationSupport implements R2dbcUpdateOperation {


    public R2dbcUpdateOperationSupport(ReactiveEntityTemplate template) {
        super(template);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.core.ReactiveUpdateOperation#update(java.lang.Class)
     */
    @NonNull
    @Override
    public R2dbcUpdate update(@NonNull Class<?> domainType) {

        Assert.notNull(domainType, "DomainType must not be null");

        return new R2dbcUpdateSupport<>(template, domainType, Query.empty(), null);
    }


    private final static class R2dbcUpdateSupport<T> extends R2dbcSupport<T, Long> implements R2dbcUpdate, TerminatingUpdate {


        R2dbcUpdateSupport(ReactiveEntityTemplate template, Class<T> domainType, Query query,
                           @Nullable SqlIdentifier tableName) {
            super(template, domainType, Long.class, query, tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveUpdateOperation.UpdateWithTable#inTable(SqlIdentifier)
         */
        @NonNull
        @Override
        public UpdateWithQuery inTable(@NonNull SqlIdentifier tableName) {

            Assert.notNull(tableName, "Table name must not be null");

            return new R2dbcUpdateSupport<>(getTemplate(), getDomainType(), getQuery(), tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveUpdateOperation.UpdateWithQuery#matching(org.springframework.data.r2dbc.query.Query)
         */
        @NonNull
        @Override
        public TerminatingUpdate matching(@NonNull Query query) {

            Assert.notNull(query, "Query must not be null");

            return new R2dbcUpdateSupport<>(getTemplate(), getDomainType(), query, getTableName());
        }

        @NonNull
        @Override
        public Mono<Long> apply(@NonNull Update update) {
            Assert.notNull(update, "Update must not be null");
            return getExecutor().doUpdate(getQuery(), update, getDomainType(), getTableName());
        }

        @Override
        public <E> Mono<E> using(E entity) {
            return getExecutor().doUpdate(entity, getTableName());
        }
    }
}
