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
import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcInsertOperation;
import com.jxpanda.r2dbc.spring.data.core.operation.executor.R2dbcInsertExecutor;
import com.jxpanda.r2dbc.spring.data.core.operation.executor.R2dbcOperationParameter;
import org.springframework.data.r2dbc.core.ReactiveInsertOperation;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.NonNull;
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

        return new R2dbcInsertSupport<>(R2dbcOperationParameter.<T, T>builder()
                .template(template)
                .domainType(domainType)
                .returnType(domainType)
                .build());
    }

    private static final class R2dbcInsertSupport<T> extends R2dbcSupport<T> implements R2dbcInsert<T> {
        private R2dbcInsertSupport(R2dbcOperationParameter<T, T> operationParameter) {
            super(operationParameter);
        }


        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveInsertOperation.InsertWithTable#into(SqlIdentifier)
         */
        @NonNull
        @Override
        public ReactiveInsertOperation.TerminatingInsert<T> into(@NonNull SqlIdentifier tableName) {

            Assert.notNull(tableName, "Table name must not be null");
            return newSupport(rebuild().tableName(tableName), R2dbcInsertSupport::new);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveInsertOperation.TerminatingInsert#one(java.lang.Object)
         */
        @NonNull
        @Override
        public Mono<T> using(@NonNull T object) {

            Assert.notNull(object, "Object to insert must not be null");

            return executorBuilder(R2dbcInsertExecutor::builder)
                    .build()
                    .execute(object);
        }

        @Override
        public Flux<T> batch(Collection<T> objectList) {
            Assert.notEmpty(objectList, "Object list to insert must not be empty");
            return executorBuilder(R2dbcInsertExecutor::builder)
                    .build()
                    .executeBatch(objectList);
        }

    }
}
