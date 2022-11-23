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
import org.springframework.data.r2dbc.core.ReactiveUpdateOperation;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

/**
 * Implementation of {@link ReactiveUpdateOperation}.
 *
 * @author Mark Paluch
 * @since 1.1
 */
public final class ReactiveUpdateOperationSupport extends ReactiveOperationSupport implements ReactiveUpdateOperation {


    public ReactiveUpdateOperationSupport(ReactiveEntityTemplate template) {
        super(template);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.core.ReactiveUpdateOperation#update(java.lang.Class)
     */
    @Nonnull
    @Override
    public ReactiveUpdateOperation.ReactiveUpdate update(@Nonnull Class<?> domainType) {

        Assert.notNull(domainType, "DomainType must not be null");

        return new ReactiveUpdateSupport<>(template, domainType, Query.empty(), null);
    }

    private final static class ReactiveUpdateSupport<T> extends ReactiveOperationSupport.ReactiveSupport<T, Long> implements ReactiveUpdate, TerminatingUpdate {


        ReactiveUpdateSupport(ReactiveEntityTemplate template, Class<T> domainType, Query query,
                              @Nullable SqlIdentifier tableName) {
            super(template, domainType, Long.class, query, tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveUpdateOperation.UpdateWithTable#inTable(SqlIdentifier)
         */
        @Nonnull
        @Override
        public UpdateWithQuery inTable(@Nonnull SqlIdentifier tableName) {

            Assert.notNull(tableName, "Table name must not be null");

            return new ReactiveUpdateSupport<>(getTemplate(), getDomainType(), getQuery(), tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveUpdateOperation.UpdateWithQuery#matching(org.springframework.data.r2dbc.query.Query)
         */
        @Nonnull
        @Override
        public TerminatingUpdate matching(@Nonnull Query query) {

            Assert.notNull(query, "Query must not be null");

            return new ReactiveUpdateSupport<>(getTemplate(), getDomainType(), query, getTableName());
        }

        @Nonnull
        @Override
        public Mono<Long> apply(@Nonnull Update update) {
            return null;
        }


//
//        /*
//         * (non-Javadoc)
//         * @see org.springframework.data.r2dbc.core.ReactiveUpdateOperation.TerminatingUpdate#apply(org.springframework.data.r2dbc.query.Update)
//         */
//        @Override
//        public Mono<Long> apply(Update update) {
//
//
//            Assert.notNull(update, "Update must not be null");
//
//            return getTemplate().doUpdate(getQuery(), update, getDomainType(), getTableName());
//        }

    }
}
