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

import com.jxpanda.r2dbc.spring.data.core.R2dbcEntityTemplate;
import com.jxpanda.r2dbc.spring.data.core.operation.ReactiveDeleteOperation;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link ReactiveDeleteOperation}.
 *
 * @author Mark Paluch
 * @since 1.1
 */
public final class ReactiveDeleteOperationSupport extends ReactiveOperationSupport implements ReactiveDeleteOperation {

    public ReactiveDeleteOperationSupport(R2dbcEntityTemplate template) {
        super(template);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.core.ReactiveDeleteOperation#delete(java.lang.Class)
     */
    @Override
    public ReactiveDelete delete(Class<?> domainType) {

        Assert.notNull(domainType, "DomainType must not be null");

        return new ReactiveDeleteSupport<>(template, domainType, Query.empty(), null);
    }

    private final static class ReactiveDeleteSupport<T> extends ReactiveOperationSupport.ReactiveSupport<T, Long> implements ReactiveDelete, TerminatingDelete {


        ReactiveDeleteSupport(R2dbcEntityTemplate template, Class<T> domainType, Query query,
                              @Nullable SqlIdentifier tableName) {

            super(template, domainType, Long.class, query, tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveDeleteOperation.DeleteWithTable#from(SqlIdentifier)
         */
        @Override
        public DeleteWithQuery from(SqlIdentifier tableName) {

            Assert.notNull(tableName, "Table name must not be null");

            return new ReactiveDeleteSupport<>(getTemplate(), getDomainType(), getQuery(), tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveDeleteOperation.DeleteWithQuery#matching(org.springframework.data.r2dbc.query.Query)
         */
        @Override
        public TerminatingDelete matching(Query query) {

            Assert.notNull(query, "Query must not be null");

            return new ReactiveDeleteSupport<>(getTemplate(), getDomainType(), query, getTableName());
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveDeleteOperation.TerminatingDelete#all()
         */
        public Mono<Long> all() {
            return getTemplate().doDelete(getQuery(), getDomainType(), getTableName());
        }

    }
}
