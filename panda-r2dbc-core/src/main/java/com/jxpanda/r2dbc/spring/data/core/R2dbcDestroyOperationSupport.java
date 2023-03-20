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

import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcDestroyOperation;
import org.springframework.data.r2dbc.core.ReactiveDeleteOperation;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.Collection;


/**
 * Implementation of {@link ReactiveDeleteOperation}.
 *
 * @author Mark Paluch
 * @since 1.1
 */
public final class R2dbcDestroyOperationSupport extends R2dbcOperationSupport implements R2dbcDestroyOperation {


    public R2dbcDestroyOperationSupport(ReactiveEntityTemplate template) {
        super(template);
    }

    @Override
    public <T> R2dbcDestroy<T> destroy(Class<T> domainType) {
        return new R2dbcDestroySupport<>(this.template, domainType);
    }


    private final static class R2dbcDestroySupport<T> extends R2dbcSupport<T> implements R2dbcDestroyOperation.R2dbcDestroy<T> {

        private final R2dbcDeleteOperationSupport.R2dbcDestroyAdapter r2DbcDestroyAdapter;

        R2dbcDestroySupport(ReactiveEntityTemplate template, Class<T> domainType) {
            super(template, domainType);
            this.r2DbcDestroyAdapter = new R2dbcDeleteOperationSupport.R2dbcDestroyAdapter(template);
        }

        R2dbcDestroySupport(ReactiveEntityTemplate template, Class<T> domainType, Query query,
                            @Nullable SqlIdentifier tableName) {
            super(template, domainType, query, tableName);
            this.r2DbcDestroyAdapter = new R2dbcDeleteOperationSupport.R2dbcDestroyAdapter(template);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveDeleteOperation.DeleteWithTable#from(SqlIdentifier)
         */
        public ReactiveDeleteOperation.DeleteWithQuery from(@NonNull SqlIdentifier tableName) {

            Assert.notNull(tableName, "Table name must not be null");

            return new R2dbcDestroySupport<>(this.reactiveEntityTemplate, this.domainType, this.query, tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveDeleteOperation.DeleteWithQuery#matching(org.springframework.data.r2dbc.query.Query)
         */
        @NonNull
        public ReactiveDeleteOperation.TerminatingDelete matching(@NonNull Query query) {

            Assert.notNull(query, "Query must not be null");

            return new R2dbcDestroySupport<>(this.reactiveEntityTemplate, this.domainType, query, this.tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveDeleteOperation.TerminatingDelete#all()
         */
        @NonNull
        @Override
        public Mono<Long> all() {
            return r2DbcDestroyAdapter.doDestroy(this.query, this.domainType, this.tableName);
        }

        @NonNull
        @Override
        public Mono<Boolean> using(T entity) {
            Assert.notNull(entity, "Entity must not be null");

            return r2DbcDestroyAdapter.doDestroy(entity, this.tableName).map(it -> it > 0);
        }

        @Override
        public <ID> Mono<Boolean> byId(ID id) {
            return r2DbcDestroyAdapter.doDestroyById(id, this.domainType);
        }

        @Override
        public <ID> Mono<Long> byIds(Collection<ID> ids) {
            return r2DbcDestroyAdapter.doDestroyByIds(ids, this.domainType);
        }


    }
}
