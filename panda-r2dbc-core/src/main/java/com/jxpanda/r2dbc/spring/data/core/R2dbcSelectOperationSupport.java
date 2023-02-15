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


import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableEntity;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.page.Page;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.page.Pagination;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.page.Paging;
import com.jxpanda.r2dbc.spring.data.core.kit.MappingKit;
import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcSelectOperation;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.criteria.EnhancedCriteria;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.springframework.data.projection.ProjectionInformation;
import org.springframework.data.r2dbc.convert.EntityRowMapper;
import org.springframework.data.r2dbc.core.ReactiveSelectOperation;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.*;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.r2dbc.core.RowsFetchSpec;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.beans.FeatureDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ReactiveSelectOperation}.
 *
 * @author Mark Paluch
 * @since 1.1
 */
public final class R2dbcSelectOperationSupport extends R2dbcOperationSupport implements ReactiveSelectOperation {

    private static final String SQL_AS = " AS ";


    public R2dbcSelectOperationSupport(ReactiveEntityTemplate template) {
        super(template);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation#select(java.lang.Class)
     */
    @NonNull
    @Override
    public <T> R2dbcSelectOperation.R2dbcSelect<T> select(@NonNull Class<T> domainType) {

        Assert.notNull(domainType, "DomainType must not be null");

        return new R2dbcSelectSupport<>(this.template, domainType, domainType);
    }


    @SuppressWarnings("SameParameterValue")
    private final static class R2dbcSelectSupport<T, R> extends R2dbcSupport<T> implements R2dbcSelectOperation.R2dbcSelect<R> {

        /**
         * 返回值类型
         */
        private final Class<R> returnType;

        public R2dbcSelectSupport(ReactiveEntityTemplate template, Class<T> domainType, Class<R> returnType) {
            super(template, domainType);
            this.returnType = returnType;
        }

        private R2dbcSelectSupport(ReactiveEntityTemplate template, Class<T> domainType, Class<R> returnType, Query query,
                                   @Nullable SqlIdentifier tableName) {
            super(template, domainType, query, tableName);
            this.returnType = returnType;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.SelectWithTable#from(java.lang.String)
         */
        @NonNull
        @Override
        public SelectWithProjection<R> from(@NonNull SqlIdentifier tableName) {

            Assert.notNull(tableName, "Table name must not be null");

            return new R2dbcSelectSupport<>(this.template, this.domainType, this.returnType, this.query, tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.SelectWithProjection#as(java.lang.Class)
         */
        @NonNull
        @Override
        public <E> SelectWithQuery<E> as(@NonNull Class<E> returnType) {

            Assert.notNull(returnType, "ReturnType must not be null");

            return new R2dbcSelectSupport<>(this.template, this.returnType, returnType, this.query, this.tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.SelectWithQuery#matching(org.springframework.data.r2dbc.query.Query)
         */
        @NonNull
        @Override
        public TerminatingSelect<R> matching(@NonNull Query query) {

            Assert.notNull(query, "Query must not be null");

            return new R2dbcSelectSupport<>(this.template, this.domainType, this.returnType, query, this.tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.TerminatingSelect#count()
         */
        @NonNull
        @Override
        public Mono<Long> count() {
            return doCount(this.query, this.domainType, this.tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.TerminatingSelect#exists()
         */
        @NonNull
        @Override
        public Mono<Boolean> exists() {
            return doExists(this.query, this.domainType, this.tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.TerminatingSelect#first()
         */
        @NonNull
        @Override
        public Mono<R> first() {
            return selectMono(this.query.limit(1), this.domainType, this.tableName, this.returnType, RowsFetchSpec::first);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.TerminatingSelect#one()
         */
        @NonNull
        @Override
        public Mono<R> one() {
            return selectMono(this.query.limit(2), this.domainType, this.tableName, this.returnType, RowsFetchSpec::one);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.TerminatingSelect#all()
         */
        @NonNull
        @Override
        public Flux<R> all() {
            return selectFlux(this.query, this.domainType, this.tableName, this.returnType, RowsFetchSpec::all);
        }

        @Override
        public Mono<Pagination<R>> paging() {
            Mono<Long> countMono = doCount(this.query, this.domainType, this.tableName);

            Mono<List<R>> recordsMono = selectFlux(this.query, this.domainType, this.tableName, this.returnType, RowsFetchSpec::all)
                    .collectList();

            return Mono.zip(countMono, recordsMono, (count, records) -> Pagination.with(this.query.getOffset(), this.query.getLimit(), count, records));
        }

        @Override
        public Mono<Pagination<R>> paging(Page page) {
            Query pageQuery = this.query.offset(page.getOffset()).limit(page.getLimit());

            Mono<Long> countMono = Mono.defer(() -> {
                if (page.isQueryCount()) {
                    return doCount(pageQuery, this.domainType, this.tableName);
                }
                return Mono.just(-1L);
            });

            Mono<List<R>> recordsMono = selectFlux(pageQuery, this.domainType, this.tableName, this.returnType, RowsFetchSpec::all)
                    .collectList();

            return Mono.zip(countMono, recordsMono, (count, records) -> new Pagination<>(page.getCurrent(), page.getSize(), count, records));
        }


        private Mono<Boolean> doExists(Query query, Class<T> entityClass, SqlIdentifier tableName) {
            return doExists(query, entityClass, tableName, false);
        }

        private Mono<Boolean> doExists(Query query, Class<T> entityClass, SqlIdentifier tableName, boolean ignoreLogicDelete) {

            RelationalPersistentEntity<T> entity = MappingKit.getRequiredEntity(entityClass);
            StatementMapper statementMapper = this.statementMapper().forType(entityClass);

            SqlIdentifier columnName = entity.hasIdProperty() ? entity.getRequiredIdProperty().getColumnName() : SqlIdentifier.unquoted("*");

            StatementMapper.SelectSpec selectSpec = statementMapper.createSelect(tableName).withProjection(columnName).limit(1);

            selectSpec = selectWithCriteria(selectSpec, query, entityClass, ignoreLogicDelete);

            PreparedOperation<?> operation = statementMapper.getMappedObject(selectSpec);

            return this.databaseClient().sql(operation).map((r, md) -> r).first().hasElement();
        }

        private Mono<Long> doCount(Query query, Class<T> entityClass, SqlIdentifier tableName) {
            return doCount(query, entityClass, tableName, false);
        }

        private Mono<Long> doCount(Query query, Class<T> entityClass, SqlIdentifier tableName, boolean ignoreLogicDelete) {

            RelationalPersistentEntity<T> entity = MappingKit.getRequiredEntity(entityClass);
            StatementMapper statementMapper = this.statementMapper().forType(entityClass);

            StatementMapper.SelectSpec selectSpec = statementMapper.createSelect(tableName).doWithTable((table, spec) -> {
                Expression countExpression = entity.hasIdProperty() ? table.column(entity.getRequiredIdProperty().getColumnName()) : Expressions.asterisk();
                return spec.withProjection(Functions.count(countExpression));
            });

            selectSpec = selectWithCriteria(selectSpec, query, entityClass, ignoreLogicDelete);

            PreparedOperation<?> operation = statementMapper.getMappedObject(selectSpec);

            return this.databaseClient().sql(operation).map((r, md) -> r.get(0, Long.class)).first().defaultIfEmpty(0L);
        }

        private RowsFetchSpec<R> doSelect(Query query, Class<T> entityClass, SqlIdentifier tableName, Class<R> returnType) {
            return doSelect(query, entityClass, tableName, returnType, false);
        }

        private RowsFetchSpec<R> doSelect(Query query, Class<T> entityClass, SqlIdentifier tableName, Class<R> returnType, boolean ignoreLogicDelete) {

            boolean isQueryEntity = false;

            if (entityClass.isAnnotationPresent(TableEntity.class)) {
                isQueryEntity = entityClass.getAnnotation(TableEntity.class).isAggregate();
            }

            StatementMapper statementMapper = isQueryEntity ? this.statementMapper() : this.statementMapper().forType(entityClass);

            StatementMapper.SelectSpec selectSpec = statementMapper.createSelect(tableName)
                    .doWithTable((table, spec) -> spec.withProjection(getSelectProjection(table, query, entityClass, returnType)));

            if (query.getLimit() > 0) {
                selectSpec = selectSpec.limit(query.getLimit());
            }

            if (query.getOffset() > 0) {
                selectSpec = selectSpec.offset(query.getOffset());
            }

            if (query.isSorted()) {
                selectSpec = selectSpec.withSort(query.getSort());
            }

            selectSpec = selectWithCriteria(selectSpec, query, entityClass, ignoreLogicDelete);

            PreparedOperation<?> operation = statementMapper.getMappedObject(selectSpec);

            return getRowsFetchSpec(this.databaseClient().sql(operation), entityClass, returnType);
        }


        private Mono<R> selectMono(Query query, Class<T> entityClass, SqlIdentifier tableName,
                                   Class<R> returnType, Function<RowsFetchSpec<R>, Mono<R>> resultHandler) {
            return resultHandler.apply(doSelect(query, entityClass, tableName, returnType))
                    .flatMap(it -> this.template.maybeCallAfterConvert(it, tableName));
        }

        private Flux<R> selectFlux(Query query, Class<T> entityClass, SqlIdentifier tableName,
                                   Class<R> returnType, Function<RowsFetchSpec<R>, Flux<R>> resultHandler) {
            return resultHandler.apply(doSelect(query, entityClass, tableName, returnType))
                    .flatMap(it -> this.template.maybeCallAfterConvert(it, tableName));
        }

        /**
         * 创建查询单元，加入了逻辑删除的判断
         */
        private static <T> StatementMapper.SelectSpec selectWithCriteria(StatementMapper.SelectSpec selectSpec, Query query, Class<T> entityClass, boolean ignoreLogicDelete) {
            Optional<CriteriaDefinition> criteriaOptional = query.getCriteria();
            if (MappingKit.isLogicDeleteEnable(entityClass, ignoreLogicDelete)) {
                criteriaOptional = query.getCriteria()
                        .or(() -> Optional.of(Criteria.empty()))
                        .map(criteriaDefinition -> {
                            // 获取查询对象中的逻辑删除字段和值，写入到criteria中
                            Pair<String, Object> logicDeleteColumn = MappingKit.getLogicDeleteColumn(entityClass, MappingKit.LogicDeleteValue.UNDELETE_VALUE);
                            if (criteriaDefinition instanceof Criteria criteria) {
                                return criteria.and(Criteria.where(logicDeleteColumn.getFirst()).is(logicDeleteColumn.getSecond()));
                            }
                            if (criteriaDefinition instanceof EnhancedCriteria enhancedCriteria) {
                                return enhancedCriteria.and(EnhancedCriteria.where(logicDeleteColumn.getFirst()).is(logicDeleteColumn.getSecond()));
                            }
                            return criteriaDefinition;
                        });
            }
            return criteriaOptional.map(selectSpec::withCriteria).orElse(selectSpec);
        }

        private <E, RT> List<Expression> getSelectProjection(Table table, Query query, Class<E> entityClass, Class<RT> returnType) {

            if (query.getColumns().isEmpty()) {

                if (returnType.isInterface()) {

                    ProjectionInformation projectionInformation = this.projectionFactory().getProjectionInformation(returnType);

                    if (projectionInformation.isClosed()) {
                        return projectionInformation.getInputProperties().stream().map(FeatureDescriptor::getName).map(table::column).collect(Collectors.toList());
                    }
                }

                RelationalPersistentEntity<E> entity = MappingKit.getRequiredEntity(entityClass);

                List<Expression> columns = new ArrayList<>();

                boolean isQueryEntity = MappingKit.isAggregateEntity(entityClass);

                entity.forEach(property -> {
                    if (MappingKit.isPropertyExists(property)) {
                        Expression expression;
                        if (!isQueryEntity) {
                            expression = table.column(property.getColumnName());
                        } else {
                            TableColumn tableColumn = property.getRequiredAnnotation(TableColumn.class);
                            if (!MappingKit.isFunctionProperty(property)) {
                                String sql = tableColumn.name();
                                // 如果设置了别名，添加别名的语法
                                if (!ObjectUtils.isEmpty(tableColumn.alias())) {
                                    sql += SQL_AS + tableColumn.alias();
                                }
                                // 如果不是函数，直接创建标准表达式
                                expression = Expressions.just(sql);
                            } else {
                                // 如果是函数，则采用函数的方式创建函数
                                expression = SimpleFunction.create(tableColumn.function(), Collections.singletonList(Expressions.just(tableColumn.name()))).as(tableColumn.alias());
                            }
                        }
                        columns.add(expression);
                    }
                });
                return columns;
            }
            return query.getColumns().stream().map(table::column).collect(Collectors.toList());
        }


        private <E> BiFunction<Row, RowMetadata, E> getRowMapper(Class<E> typeToRead) {
            return new EntityRowMapper<>(typeToRead, this.converter());
        }


        private <E, RT> RowsFetchSpec<RT> getRowsFetchSpec(DatabaseClient.GenericExecuteSpec executeSpec, Class<E> entityClass, Class<RT> returnType) {

            boolean simpleType;

            BiFunction<Row, RowMetadata, RT> rowMapper;
            if (returnType.isInterface()) {
                simpleType = this.converter().isSimpleType(entityClass);
                rowMapper = getRowMapper(entityClass).andThen(source -> this.projectionFactory().createProjection(returnType, source));
            } else {
                simpleType = this.converter().isSimpleType(returnType);
                rowMapper = getRowMapper(returnType);
            }

            // avoid top-level null values if the read type is a simple one (e.g. SELECT MAX(age) via Integer.class)
            if (simpleType) {
                return new UnwrapOptionalFetchSpecAdapter<>(executeSpec.map((row, metadata) -> Optional.ofNullable(rowMapper.apply(row, metadata))));
            }

            return executeSpec.map(rowMapper);
        }

        private record UnwrapOptionalFetchSpecAdapter<T>(
                RowsFetchSpec<Optional<T>> delegate) implements RowsFetchSpec<T> {

            @NonNull
            @Override
            public Mono<T> one() {
                return delegate.one().handle((optional, sink) -> optional.ifPresent(sink::next));
            }

            @NonNull
            @Override
            public Mono<T> first() {
                return delegate.first().handle((optional, sink) -> optional.ifPresent(sink::next));
            }

            @NonNull
            @Override
            public Flux<T> all() {
                return delegate.all().handle((optional, sink) -> optional.ifPresent(sink::next));
            }
        }

    }
}
