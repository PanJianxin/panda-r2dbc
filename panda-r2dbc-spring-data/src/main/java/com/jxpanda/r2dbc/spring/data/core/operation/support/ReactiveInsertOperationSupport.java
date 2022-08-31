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
import com.jxpanda.r2dbc.spring.data.core.operation.ReactiveInsertOperation;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link ReactiveInsertOperation}.
 *
 * @author Mark Paluch
 * @since 1.1
 */
public final class ReactiveInsertOperationSupport extends ReactiveOperationSupport implements ReactiveInsertOperation {

    public ReactiveInsertOperationSupport(R2dbcEntityTemplate template) {
        super(template);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.core.ReactiveInsertOperation#insert(java.lang.Class)
     */
    @Override
    public <T> ReactiveInsert<T> insert(Class<T> domainType) {

        Assert.notNull(domainType, "DomainType must not be null");

        return new ReactiveInsertSupport<>(this.template, domainType, null);
    }

    private final static class ReactiveInsertSupport<T> extends ReactiveOperationSupport.ReactiveSupport<T, T> implements ReactiveInsert<T> {

        ReactiveInsertSupport(R2dbcEntityTemplate template, Class<T> domainType, @Nullable SqlIdentifier tableName) {
            super(template, domainType, domainType, null, tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveInsertOperation.InsertWithTable#into(SqlIdentifier)
         */
        @Override
        public TerminatingInsert<T> into(SqlIdentifier tableName) {

            Assert.notNull(tableName, "Table name must not be null");

            return new ReactiveInsertSupport<>(getTemplate(), getDomainType(), tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveInsertOperation.TerminatingInsert#one(java.lang.Object)
         */
        @Override
        public Mono<T> using(T object) {

            Assert.notNull(object, "Object to insert must not be null");

            return getTemplate().doInsert(object, getTableName());
        }

    }
}
