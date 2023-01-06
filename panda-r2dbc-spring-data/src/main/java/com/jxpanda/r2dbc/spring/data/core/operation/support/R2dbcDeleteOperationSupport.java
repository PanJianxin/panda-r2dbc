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
import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcDeleteOperation;
import org.springframework.data.r2dbc.core.ReactiveDeleteOperation;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

/**
 * Implementation of {@link ReactiveDeleteOperation}.
 *
 * @author Mark Paluch
 * @since 1.1
 */
public final class R2dbcDeleteOperationSupport extends R2dbcOperationSupport implements ReactiveDeleteOperation {

    public R2dbcDeleteOperationSupport(ReactiveEntityTemplate template) {
        super(template);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.core.ReactiveDeleteOperation#delete(java.lang.Class)
     */
    @Nonnull
    @Override
    public R2dbcDeleteOperation.R2dbcDelete delete(@Nonnull Class<?> domainType) {

        Assert.notNull(domainType, "DomainType must not be null");

        return new R2dbcDeleteSupport<>(template, domainType, Query.empty(), null);
    }

    private final static class R2dbcDeleteSupport<T> extends R2dbcSupport<T, Long> implements R2dbcDeleteOperation.R2dbcDelete {


        R2dbcDeleteSupport(ReactiveEntityTemplate template, Class<T> domainType, Query query,
                           @Nullable SqlIdentifier tableName) {

            super(template, domainType, Long.class, query, tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveDeleteOperation.DeleteWithTable#from(SqlIdentifier)
         */
        @Nonnull
        @Override
        public DeleteWithQuery from(@Nonnull SqlIdentifier tableName) {

            Assert.notNull(tableName, "Table name must not be null");

            return new R2dbcDeleteSupport<>(getTemplate(), getDomainType(), getQuery(), tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveDeleteOperation.DeleteWithQuery#matching(org.springframework.data.r2dbc.query.Query)
         */
        @Nonnull
        @Override
        public TerminatingDelete matching(@Nonnull Query query) {

            Assert.notNull(query, "Query must not be null");

            return new R2dbcDeleteSupport<>(getTemplate(), getDomainType(), query, getTableName());
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveDeleteOperation.TerminatingDelete#all()
         */
        @Nonnull
        @Override
        public Mono<Long> all() {
            return getExecutor().doDelete(getQuery(), getDomainType(), getTableName());
        }

        @Override
        public <E> Mono<Boolean> using(E entity) {
            Assert.notNull(entity, "Entity must not be null");

            return getExecutor().doDelete(entity, getTableName()).map(it -> it > 0);
        }
    }
}
