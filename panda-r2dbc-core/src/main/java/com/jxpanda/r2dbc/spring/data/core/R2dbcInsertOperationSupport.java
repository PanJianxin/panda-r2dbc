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

import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcInsertOperation;
import org.springframework.data.r2dbc.core.ReactiveInsertOperation;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;


/**
 * Implementation of {@link ReactiveInsertOperation}.
 *
 * @author Mark Paluch
 * @since 1.1
 */
public final class R2dbcInsertOperationSupport extends R2dbcOperationSupport implements R2dbcInsertOperation {

    public R2dbcInsertOperationSupport(ReactiveEntityTemplate template) {
        super(template);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.core.ReactiveInsertOperation#insert(java.lang.Class)
     */
    @NonNull
    @Override
    public <T> R2dbcInsert<T> insert(@NonNull Class<T> domainType) {

        Assert.notNull(domainType, "DomainType must not be null");

        return new R2dbcInsertSupport<>(this.template, domainType, null);
    }

    private final static class R2dbcInsertSupport<T> extends R2dbcSupport<T, T> implements R2dbcInsert<T> {

        R2dbcInsertSupport(ReactiveEntityTemplate template, Class<T> domainType, @Nullable SqlIdentifier tableName) {
            super(template, domainType, domainType, null, tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveInsertOperation.InsertWithTable#into(SqlIdentifier)
         */
        @NonNull
        @Override
        public ReactiveInsertOperation.TerminatingInsert<T> into(@NonNull SqlIdentifier tableName) {

            Assert.notNull(tableName, "Table name must not be null");

            return new R2dbcInsertSupport<>(getTemplate(), getDomainType(), tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveInsertOperation.TerminatingInsert#one(java.lang.Object)
         */
        @NonNull
        @Override
        public Mono<T> using(@NonNull T object) {

            Assert.notNull(object, "Object to insert must not be null");

            return getExecutor().doInsert(object, getTableName());
        }

        @Override
        public Flux<T> batchInsert(Collection<T> objectList) {
            Assert.notEmpty(objectList, "Object list to insert must not be empty");
            return getExecutor().doBatchInsert(objectList, getTableName());
        }
    }
}
