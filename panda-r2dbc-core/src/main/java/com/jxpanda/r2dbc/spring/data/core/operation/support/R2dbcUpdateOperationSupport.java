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
import com.jxpanda.r2dbc.spring.data.core.kit.R2dbcMappingKit;
import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcUpdateOperation;
import com.jxpanda.r2dbc.spring.data.core.operation.executor.R2dbcOperationParameter;
import com.jxpanda.r2dbc.spring.data.core.operation.executor.R2dbcUpdateExecutor;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.r2dbc.core.ReactiveUpdateOperation;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

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

    /**
     * (non-Javadoc)
     *
     * @see org.springframework.data.r2dbc.core.ReactiveUpdateOperation#update(java.lang.Class)
     */
    @NonNull
    @Override
    public <T> R2dbcUpdate<T> update(@NonNull Class<T> domainType) {

        Assert.notNull(domainType, "DomainType must not be null");


        return new R2dbcUpdateSupport<>(R2dbcOperationParameter.<T, T>builder()
                .template(template)
                .domainType(domainType)
                .returnType(domainType)
                .build());
    }


    private static final class R2dbcUpdateSupport<T> extends R2dbcSupport<T> implements R2dbcUpdate<T>, ReactiveUpdateOperation.TerminatingUpdate {


        private R2dbcUpdateSupport(R2dbcOperationParameter<T, T> operationParameter) {
            super(operationParameter);
        }


        /**
         * (non-Javadoc)
         *
         * @see org.springframework.data.r2dbc.core.ReactiveUpdateOperation.UpdateWithTable#inTable(SqlIdentifier)
         */
        @NonNull
        @Override
        public ReactiveUpdateOperation.UpdateWithQuery inTable(@NonNull SqlIdentifier tableName) {

            Assert.notNull(tableName, "Table name must not be null");

            return newSupport(rebuild().tableName(tableName), R2dbcUpdateSupport::new);
        }

        /**
         * (non-Javadoc)
         *
         * @see org.springframework.data.r2dbc.core.ReactiveUpdateOperation.UpdateWithQuery#matching(Query)
         */
        @NonNull
        @Override
        public ReactiveUpdateOperation.TerminatingUpdate matching(@NonNull Query query) {

            Assert.notNull(query, "Query must not be null");

            return newSupport(rebuild().query(query), R2dbcUpdateSupport::new);
        }

        @NonNull
        @Override
        public Mono<Long> apply(@NonNull Update update) {
            Assert.notNull(update, "Update must not be null");
            return executorBuilder(R2dbcUpdateExecutor::<T, Long>builder)
                    .returnType(Long.class)
                    .updateSupplier(() -> update)
                    .build()
                    .execute();

        }

        @Override
        public Mono<T> using(T entity) {
            return executorBuilder(R2dbcUpdateExecutor::<T, T>builder)
                    .build()
                    .execute(entity);
        }

        @Override
        public Flux<T> batch(Collection<T> objectList) {
            return executorBuilder(R2dbcUpdateExecutor::<T, T>builder)
                    .build()
                    .executeBatch(objectList);
        }

    }
}
