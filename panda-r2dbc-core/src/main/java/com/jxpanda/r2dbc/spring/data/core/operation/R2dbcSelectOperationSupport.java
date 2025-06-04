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
import com.jxpanda.r2dbc.spring.data.core.enhance.query.page.Pagination;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker.Seeker;
import com.jxpanda.r2dbc.spring.data.core.kit.QueryKit;
import com.jxpanda.r2dbc.spring.data.core.operation.contract.R2dbcSelectOperation;
import com.jxpanda.r2dbc.spring.data.core.operation.executor.R2dbcOperationOption;
import com.jxpanda.r2dbc.spring.data.core.operation.executor.R2dbcOperationContext;
import com.jxpanda.r2dbc.spring.data.core.operation.executor.R2dbcSelectExecutor;
import org.reactivestreams.Publisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.ReactiveSelectOperation;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.Expressions;
import org.springframework.data.relational.core.sql.Functions;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.NonNull;
import org.springframework.r2dbc.core.RowsFetchSpec;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.function.Function;

/**
 * Implementation of {@link ReactiveSelectOperation}.
 *
 * @author Mark Paluch
 * @since 1.1
 */
public final class R2dbcSelectOperationSupport extends R2dbcOperationSupport implements ReactiveSelectOperation {


    public R2dbcSelectOperationSupport(ReactiveEntityTemplate template) {
        super(template);
    }

    /**
     * (non-Javadoc)
     *
     * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation#select(java.lang.Class)
     */
    @NonNull
    @Override
    public <T> R2dbcSelectOperation.R2dbcSelect<T> select(@NonNull Class<T> entityType) {

        Assert.notNull(entityType, "entityType must not be null");
        return new R2dbcSelectSupport<>(R2dbcOperationContext.<T, T>builder()
                .template(template)
                .entityType(entityType)
                .resultType(entityType)
                .build());
    }


    @SuppressWarnings("SameParameterValue")
    private static final class R2dbcSelectSupport<T> extends R2dbcSupport<T> implements R2dbcSelectOperation.R2dbcSelect<T> {
        private R2dbcSelectSupport(R2dbcOperationContext<T, T> operationContext) {
            super(operationContext);
        }


        private R2dbcSelectSupport(R2dbcOperationContext.R2dbcOperationContextBuilder<T, T> contextBuilder) {
            super(contextBuilder);
        }

        private R2dbcSelectSupport<T> newSupport(R2dbcOperationContext.R2dbcOperationContextBuilder<T, T> contextBuilder) {
            return new R2dbcSelectSupport<>(contextBuilder);
        }

        @Override
        public R2dbcSelectOperation.R2dbcSelect<T> withOption(R2dbcOperationOption option) {
            return newSupport(rebuilder().option(option));
        }


        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.SelectWithTable#from(java.lang.String)
         */
        @NonNull
        @Override
        public SelectWithProjection<T> from(@NonNull SqlIdentifier tableName) {

            Assert.notNull(tableName, "Table name must not be null");

            return newSupport(rebuilder().tableName(tableName));
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.SelectWithProjection#as(java.lang.Class)
         */
        @NonNull
        @Override
        public <E> SelectWithQuery<E> as(@NonNull Class<E> resultType) {

            Assert.notNull(resultType, "resultType must not be null");

            return newSupport(rebuilder(resultType, resultType), R2dbcSelectSupport::new);
        }

        @Override
        @NonNull
        public SelectWithQuery<T> withFetchSize(int fetchSize) {
            return newSupport(rebuilder().filterFunction(statement -> statement.fetchSize(fetchSize)), R2dbcSelectSupport::new);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.SelectWithQuery#matching(org.springframework.data.r2dbc.query.Query)
         */
        @NonNull
        @Override
        public R2dbcSelectOperation.TerminatingSelect<T> matching(@NonNull Query query) {

            Assert.notNull(query, "Query must not be null");

            return newSupport(rebuilder().query(query));
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.TerminatingSelect#count()
         */
        @NonNull
        @Override
        public Mono<Long> count() {
            return doCount();
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.TerminatingSelect#exists()
         */
        @NonNull
        @Override
        public Mono<Boolean> exists() {
            return doExists();
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.TerminatingSelect#first()
         */
        @NonNull
        @Override
        public Mono<T> first() {
            return doSelect(operationContext -> operationContext.getQuery().limit(1), RowsFetchSpec::first);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.TerminatingSelect#one()
         */
        @NonNull
        @Override
        public Mono<T> one() {
            return doSelect(operationContext -> operationContext.getQuery().limit(1), RowsFetchSpec::one);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.TerminatingSelect#all()
         */
        @NonNull
        @Override
        public Flux<T> all() {
            return doSelect(RowsFetchSpec::all);
        }

        @Override
        public <ID> Mono<T> byId(ID id) {
            return doSelect(operationContext -> QueryKit.queryById(operationContext.getEntityType(), id), RowsFetchSpec::one);
        }

        @Override
        public <ID> Flux<T> byIds(Collection<ID> ids) {
            return doSelect(operationContext -> QueryKit.queryByIds(operationContext.getEntityType(), ids), RowsFetchSpec::all);
        }

        @Override
        public Mono<Pagination<T>> page(Pageable pageable) {
            return doPage(pageable, R2dbcOperationContext::getQuery);
        }

        @Override
        public Mono<Pagination<T>> seek(Seeker<T> seeker) {
            return doPage(seeker.takePageable(), operationContext -> seeker.buildQuery(operationContext.getEntityType()));
        }


        private Mono<Boolean> doExists() {
            return executorBuilder(R2dbcSelectExecutor::<T, Boolean>builder)
                    .specBuilder(operationContext -> {
                        RelationalPersistentEntity<T> persistentEntity = operationContext.getRelationalPersistentEntity();
                        SqlIdentifier columnName = persistentEntity.hasIdProperty() ? persistentEntity.getRequiredIdProperty().getColumnName() : SqlIdentifier.unquoted("*");
                        return operationContext.getStatementMapper().createSelect(operationContext.getTableName())
                                .doWithTable((table, spec) -> spec.withProjection(columnName))
                                .limit(1);
                    })
                    .rowMapperBuilder(operationContext -> (row, rowMetadata) -> row != null)
                    .build()
                    .execute(rowsFetchSpec -> rowsFetchSpec.first().hasElement());
        }

        private Mono<Long> doCount() {
            return doCount(null);
        }

        private Mono<Long> doCount(Function<R2dbcOperationContext<T, Long>, Query> queryHandler) {
            return executorBuilder(R2dbcSelectExecutor::<T, Long>builder)
                    .queryHandler(queryHandler)
                    .specBuilder(operationContext -> operationContext.getStatementMapper().createSelect(operationContext.getTableName())
                            .doWithTable((table, spec) -> spec.withProjection(Functions.count(Expressions.asterisk()))))
                    .rowMapperBuilder(operationContext -> (row, rowMetadata) -> row.get(0, Long.class))
                    .build()
                    .execute(rowsFetchSpec -> rowsFetchSpec.first().defaultIfEmpty(0L));
        }


        private <P extends Publisher<T>> P doSelect(Function<RowsFetchSpec<T>, P> resultHandler) {
            return doSelect(null, resultHandler);
        }

        private <P extends Publisher<T>> P doSelect(Function<R2dbcOperationContext<T, T>, Query> queryHandler, Function<RowsFetchSpec<T>, P> resultHandler) {
            return executorBuilder(R2dbcSelectExecutor::<T, T>builder)
                    .queryHandler(queryHandler)
                    .build()
                    .execute(resultHandler);
        }

        private Mono<Pagination<T>> doPage(Pageable pageable, Function<R2dbcOperationContext<T, T>, Query> queryHandler) {
            return doSelect(queryHandler, RowsFetchSpec::all)
                    .collectList()
                    .flatMap(records -> {
                        // 判断是否需要做count查询
                        // 判定规则如下：
                        boolean isNeedCount =
                                // 1、分页没有被禁用，这三个条件判断分页有没有被禁用
                                pageable.isPaged() && pageable.getPageSize() > 0
                                // 2、查询回来的数据不为空（还有数据）
                                && !records.isEmpty()
                                // 3、可能还有下一页（页长等于查询回来的数据长度，则可能还有下一页，页长如果大于数据长度，则肯定没有下一页了）
                                && pageable.getPageSize() <= records.size();

                        Mono<Long> totalSupplier = Mono.defer(() -> {
                            // 分页没有被禁用，且可能还有下一页数据，则需要做count查询
                            if (isNeedCount) {
                                // 调用count函数返回一共有多少条数据
                                return doCount(operationContext -> operationContext.getQuery().with(pageable));
                            }
                            // 如果不需要查询，就返回offset+record.size()
                            return Mono.just(pageable.getOffset() + records.size());
                        });

                        // count以下共有多少条数据之后再返回分页对象
                        return totalSupplier.map(total -> new Pagination<>(records, pageable, total));
                    });
        }

    }

}
