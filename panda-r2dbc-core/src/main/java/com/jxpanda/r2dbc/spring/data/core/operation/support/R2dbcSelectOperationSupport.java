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
import com.jxpanda.r2dbc.spring.data.core.enhance.query.page.Pagination;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker.Seeker;
import com.jxpanda.r2dbc.spring.data.core.kit.QueryKit;
import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcSelectOperation;
import com.jxpanda.r2dbc.spring.data.core.operation.executor.R2dbcOperationOption;
import com.jxpanda.r2dbc.spring.data.core.operation.executor.R2dbcOperationParameter;
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
    public <T> R2dbcSelectOperation.R2dbcSelect<T> select(@NonNull Class<T> domainType) {

        Assert.notNull(domainType, "DomainType must not be null");
        return new R2dbcSelectSupport<>(R2dbcOperationParameter.<T, T>builder()
                .template(template)
                .domainType(domainType)
                .returnType(domainType)
                .build());
    }


    @SuppressWarnings("SameParameterValue")
    private static final class R2dbcSelectSupport<T> extends R2dbcSupport<T> implements R2dbcSelectOperation.R2dbcSelect<T> {
        private R2dbcSelectSupport(R2dbcOperationParameter<T, T> parameter) {
            super(parameter);
        }


        private R2dbcSelectSupport(R2dbcOperationParameter.R2dbcOperationParameterBuilder<T, T> parameterBuilder) {
            super(parameterBuilder);
        }

        private R2dbcSelectSupport<T> newSupport(R2dbcOperationParameter.R2dbcOperationParameterBuilder<T, T> parameterBuilder) {
            return new R2dbcSelectSupport<>(parameterBuilder);
        }

        @Override
        public R2dbcSelectOperation.R2dbcSelect<T> withOption(R2dbcOperationOption option) {
            return newSupport(rebuild().option(option));
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.SelectWithTable#from(java.lang.String)
         */
        @NonNull
        @Override
        public SelectWithProjection<T> from(@NonNull SqlIdentifier tableName) {

            Assert.notNull(tableName, "Table name must not be null");

            return newSupport(rebuild().tableName(tableName));
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.SelectWithProjection#as(java.lang.Class)
         */
        @NonNull
        @Override
        public <E> SelectWithQuery<E> as(@NonNull Class<E> returnType) {

            Assert.notNull(returnType, "ReturnType must not be null");

            return newSupport(rebuild(returnType, returnType), R2dbcSelectSupport::new);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.SelectWithQuery#matching(org.springframework.data.r2dbc.query.Query)
         */
        @NonNull
        @Override
        public R2dbcSelectOperation.TerminatingSelect<T> matching(@NonNull Query query) {

            Assert.notNull(query, "Query must not be null");

            return newSupport(rebuild().query(query));
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
            return doSelect(parameter -> parameter.getQuery().limit(1), RowsFetchSpec::first);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.TerminatingSelect#one()
         */
        @NonNull
        @Override
        public Mono<T> one() {
            return doSelect(parameter -> parameter.getQuery().limit(1), RowsFetchSpec::one);
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
            return doSelect(parameter -> QueryKit.queryById(parameter.getDomainType(), id), RowsFetchSpec::one);
        }

        @Override
        public <ID> Flux<T> byIds(Collection<ID> ids) {
            return doSelect(parameter -> QueryKit.queryByIds(parameter.getDomainType(), ids), RowsFetchSpec::all);
        }

        @Override
        public Mono<Pagination<T>> page(Pageable pageable) {
            return doPage(pageable, R2dbcOperationParameter::getQuery);
        }

        @Override
        public Mono<Pagination<T>> seek(Seeker<T> seeker) {
            return doPage(seeker.takePageable(), parameter -> seeker.buildQuery(parameter.getDomainType()));
        }


        private Mono<Boolean> doExists() {
            return executorBuilder(R2dbcSelectExecutor::<T, Boolean>builder)
                    .specBuilder(parameter -> {
                        RelationalPersistentEntity<T> persistentEntity = parameter.getRelationalPersistentEntity();
                        SqlIdentifier columnName = persistentEntity.hasIdProperty() ? persistentEntity.getRequiredIdProperty().getColumnName() : SqlIdentifier.unquoted("*");
                        return parameter.getStatementMapper().createSelect(parameter.getTableName())
                                .doWithTable((table, spec) -> spec.withProjection(columnName))
                                .limit(1);
                    })
                    .rowMapperBuilder(parameter -> (row, rowMetadata) -> row != null)
                    .build()
                    .execute(rowsFetchSpec -> rowsFetchSpec.first().hasElement());
        }

        private Mono<Long> doCount() {
            return doCount(null);
        }

        private Mono<Long> doCount(Function<R2dbcOperationParameter<T, Long>, Query> queryHandler) {
            return executorBuilder(R2dbcSelectExecutor::<T, Long>builder)
                    .queryHandler(queryHandler)
                    .specBuilder(parameter -> parameter.getStatementMapper().createSelect(parameter.getTableName())
                            .doWithTable((table, spec) -> spec.withProjection(Functions.count(Expressions.asterisk()))))
                    .rowMapperBuilder(parameter -> (row, rowMetadata) -> row.get(0, Long.class))
                    .build()
                    .execute(rowsFetchSpec -> rowsFetchSpec.first().defaultIfEmpty(0L));
        }


        private <P extends Publisher<T>> P doSelect(Function<RowsFetchSpec<T>, P> resultHandler) {
            return doSelect(null, resultHandler);
        }

        private <P extends Publisher<T>> P doSelect(Function<R2dbcOperationParameter<T, T>, Query> queryHandler, Function<RowsFetchSpec<T>, P> resultHandler) {
            return executorBuilder(R2dbcSelectExecutor::<T, T>builder)
                    .queryHandler(queryHandler)
                    .build()
                    .execute(resultHandler);
        }

        private Mono<Pagination<T>> doPage(Pageable pageable, Function<R2dbcOperationParameter<T, T>, Query> queryHandler) {
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
                                return doCount(parameter -> parameter.getQuery().with(pageable));
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
