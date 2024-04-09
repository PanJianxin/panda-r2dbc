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
import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcSelectOperation;
import com.jxpanda.r2dbc.spring.data.core.operation.executor.R2dbcOperationOption;
import com.jxpanda.r2dbc.spring.data.core.operation.executor.R2dbcOperationParameter;
import com.jxpanda.r2dbc.spring.data.core.operation.executor.R2dbcSelectExecutor;
import org.reactivestreams.Publisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
        public Mono<Page<T>> page(Pageable pageable) {

            Mono<Long> totalSupplier = Mono.defer(() -> {
                if (pageable.isPaged()) {
                    return doCount(parameter -> parameter.getQuery().with(pageable));
                }
                return Mono.just(-1L);
            });

            return doSelect(parameter -> parameter.getQuery().with(pageable), RowsFetchSpec::all)
                    .collectList()
                    .flatMap(content -> {
                        // 有两种情况不需要count查询
                        // 1、不支持分页 [或] （查询的是第一页数据 [且] 第一页数据的长度小于页长）
                        // 2、支持分页，但是查询的是最后一页数据，可以通过最后一页数据的长度+偏移量offset计算得到总数据量

                        // 不支持分页，或者查询的是第一页数据，且第一页数据的长度小于页长
                        if (pageable.isUnpaged() || (pageable.getOffset() == 0 && pageable.getPageSize() > content.size())) {
                            return Mono.just(new PageImpl<>(content, pageable, content.size()));
                        }

                        // 这里的意思是：如果是最后一页数据了，就不用count查询了，可以直接使用数据计算出共有多少条数据
                        if (!content.isEmpty() && pageable.getPageSize() > content.size()) {
                            return Mono.just(new PageImpl<>(content, pageable, pageable.getOffset() + content.size()));
                        }

                        // count以下共有多少条数据之后再返回分页对象
                        return totalSupplier.map(total -> new PageImpl<>(content, pageable, total));
                    });
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


//        private RowsFetchSpec<T> doSelect(Query query, Class<T> entityClass, SqlIdentifier tableName, Class<T> returnType) {
//
//            // 是否是聚合对象
//            boolean isAggregate = false;
//
//            if (entityClass.isAnnotationPresent(TableEntity.class)) {
//                isAggregate = entityClass.getAnnotation(TableEntity.class).aggregate();
//            }
//
//            StatementMapper statementMapper = isAggregate ? this.statementMapper() : this.statementMapper().forType(entityClass);
//
//            StatementMapper.SelectSpec selectSpec = statementMapper.createSelect(tableName)
//                    .doWithTable((table, spec) -> spec.withProjection(getSelectProjection(table, query, entityClass, returnType)));
//
//            if (query.getLimit() > 0) {
//                selectSpec = selectSpec.limit(query.getLimit());
//            }
//
//            if (query.getOffset() > 0) {
//                selectSpec = selectSpec.offset(query.getOffset());
//            }
//
//            if (query.isSorted()) {
//                selectSpec = selectSpec.withSort(query.getSort());
//            }
//
//            selectSpec = selectWithCriteria(selectSpec, query, entityClass);
//
//            PreparedOperation<?> operation = statementMapper.getMappedObject(selectSpec);
//
//            return operationParameter.getTemplate().getRowsFetchSpec(operationParameter.getTemplate().getDatabaseClient().sql(operation), entityClass, returnType);
//        }


//        private Mono<T> selectMono(Query query, Class<T> entityClass, SqlIdentifier tableName,
//                                   Class<T> returnType, Function<RowsFetchSpec<T>, Mono<T>> resultHandler) {
//            return resultHandler.apply(doSelect(query, entityClass, tableName, returnType))
//                    .flatMap(result -> selectReference(entityClass, result))
//                    .flatMap(it -> this.template.maybeCallAfterConvert(it, tableName));
//        }
//
//        private Flux<T> selectFlux(Query query, Class<T> entityClass, SqlIdentifier tableName,
//                                   Class<T> returnType, Function<RowsFetchSpec<T>, Flux<T>> resultHandler) {
//            return resultHandler.apply(doSelect(query, entityClass, tableName, returnType))
//                    .flatMap(result -> selectReference(entityClass, result))
//                    .flatMap(it -> this.template.maybeCallAfterConvert(it, tableName));
//        }


    }

}
