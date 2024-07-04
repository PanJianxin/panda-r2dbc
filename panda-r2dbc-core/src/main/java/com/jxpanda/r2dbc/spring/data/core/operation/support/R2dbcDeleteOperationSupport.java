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
import com.jxpanda.r2dbc.spring.data.core.kit.QueryKit;
import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcDeleteOperation;
import com.jxpanda.r2dbc.spring.data.core.operation.executor.R2dbcDeleteExecutor;
import com.jxpanda.r2dbc.spring.data.core.operation.executor.R2dbcOperationParameter;
import org.springframework.data.r2dbc.core.ReactiveDeleteOperation;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.Collection;


/**
 * Implementation of {@link ReactiveDeleteOperation}.
 *
 * @author Mark Paluch
 * @since 1.1
 */
public final class R2dbcDeleteOperationSupport extends R2dbcOperationSupport implements R2dbcDeleteOperation {

    public R2dbcDeleteOperationSupport(ReactiveEntityTemplate template) {
        super(template);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.core.ReactiveDeleteOperation#deleteValue(java.lang.Class)
     */
    @NonNull
    @Override
    public <T> R2dbcDeleteOperation.R2dbcDelete<T> delete(@NonNull Class<T> domainType) {

        Assert.notNull(domainType, "DomainType must not be null");

        return new R2dbcDeleteSupport<>(R2dbcOperationParameter.<T, T>builder()
                .template(template)
                .domainType(domainType)
                .returnType(domainType)
                .build());
    }


    private static final class R2dbcDeleteSupport<T> extends R2dbcSupport<T> implements R2dbcDeleteOperation.R2dbcDelete<T> {


        private R2dbcDeleteSupport(R2dbcOperationParameter<T, T> operationParameter) {
            super(operationParameter);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveDeleteOperation.DeleteWithTable#from(SqlIdentifier)
         */
        @NonNull
        @Override
        public ReactiveDeleteOperation.DeleteWithQuery from(@NonNull SqlIdentifier tableName) {

            Assert.notNull(tableName, "Table name must not be null");

//            return new R2dbcDeleteSupport<>(this.template, this.domainType, this.query, tableName);
            return newSupport(rebuild().tableName(tableName), R2dbcDeleteSupport::new);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveDeleteOperation.DeleteWithQuery#matching(org.springframework.data.r2dbc.query.Query)
         */
        @NonNull
        @Override
        public ReactiveDeleteOperation.TerminatingDelete matching(@NonNull Query query) {

            Assert.notNull(query, "Query must not be null");

//            return new R2dbcDeleteSupport<>(this.template, this.domainType, query, this.tableName);
            return newSupport(rebuild().query(query), R2dbcDeleteSupport::new);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveDeleteOperation.TerminatingDelete#all()
         */
        @NonNull
        @Override
        public Mono<Long> all() {
            return executorBuilder(R2dbcDeleteExecutor::<T, Long>builder)
                    .returnType(Long.class)
                    .build()
                    .execute();
        }

        @Override
        public Mono<Boolean> using(T entity) {
            Assert.notNull(entity, "Entity must not be null");
            return executorBuilder(R2dbcDeleteExecutor::<T, Long>builder)
                    .returnType(Long.class)
                    .build()
                    .execute(entity)
                    .map(it -> it > 0);
        }

        @Override
        public <ID> Mono<Boolean> byId(ID id) {
            Assert.notNull(id, "ID must not be empty");
            return executorBuilder(R2dbcDeleteExecutor::<T, Long>builder)
                    .returnType(Long.class)
                    .queryHandler(parameter -> QueryKit.queryById(parameter.getDomainType(), id))
                    .build()
                    .execute()
                    .map(it -> it > 0);
        }

        @Override
        public <ID> Mono<Long> byIds(Collection<ID> ids) {
            Assert.notEmpty(ids, "ID collection must not be empty");
            return executorBuilder(R2dbcDeleteExecutor::<T, Long>builder)
                    .returnType(Long.class)
                    .queryHandler(parameter -> QueryKit.queryById(parameter.getDomainType(), ids))
                    .build()
                    .execute();
        }

    }

}
