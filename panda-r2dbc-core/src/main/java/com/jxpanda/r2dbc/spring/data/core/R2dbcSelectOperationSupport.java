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
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableReference;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcOperationArgs;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcOperationExecutor;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.criteria.EnhancedCriteria;
import com.jxpanda.r2dbc.spring.data.core.kit.QueryKit;
import com.jxpanda.r2dbc.spring.data.core.kit.R2dbcMappingKit;
import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcSelectOperation;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.CollectionKit;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.ReflectionKit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.projection.ProjectionInformation;
import org.springframework.data.r2dbc.core.ReactiveSelectOperation;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.*;
import org.springframework.data.util.Pair;
import org.springframework.data.util.StreamUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.r2dbc.core.RowsFetchSpec;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.beans.FeatureDescriptor;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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
    private static final class R2dbcSelectSupport<T, R> extends R2dbcSupport<T> implements R2dbcSelectOperation.R2dbcSelect<R> {

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

            return new R2dbcSelectSupport<>(this.reactiveEntityTemplate, this.domainType, this.returnType, this.query, tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.SelectWithProjection#as(java.lang.Class)
         */
        @NonNull
        @Override
        public <E> SelectWithQuery<E> as(@NonNull Class<E> returnType) {

            Assert.notNull(returnType, "ReturnType must not be null");

            return new R2dbcSelectSupport<>(this.reactiveEntityTemplate, this.returnType, returnType, this.query, this.tableName);
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.r2dbc.core.ReactiveSelectOperation.SelectWithQuery#matching(org.springframework.data.r2dbc.query.Query)
         */
        @NonNull
        @Override
        public R2dbcSelectOperation.TerminatingSelect<R> matching(@NonNull Query query) {

            Assert.notNull(query, "Query must not be null");

            return new R2dbcSelectSupport<>(this.reactiveEntityTemplate, this.domainType, this.returnType, query, this.tableName);
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
        public <ID> Mono<R> byId(ID id) {
            Query query = QueryKit.queryById(this.domainType, id);
            return selectMono(query, this.domainType, this.tableName, this.returnType, RowsFetchSpec::one);
        }

        @Override
        public <ID> Flux<R> byIds(Collection<ID> ids) {
            Query query = QueryKit.queryByIds(this.domainType, ids);
            return selectFlux(query, this.domainType, this.tableName, this.returnType, RowsFetchSpec::all);
        }

        @Override
        public Mono<Page<R>> page(Pageable pageable) {

            Query pageQuery = query.with(pageable);

            Mono<Long> totalSupplier = Mono.defer(() -> {
                if (pageable.isPaged()) {
                    return doCount(pageQuery, this.domainType, this.tableName);
                }
                return Mono.just(-1L);
            });

            return selectFlux(pageQuery, this.domainType, this.tableName, this.returnType, RowsFetchSpec::all)
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


        private Mono<Boolean> doExists(Query query, Class<T> entityClass, SqlIdentifier tableName) {

            RelationalPersistentEntity<T> entity = R2dbcMappingKit.getRequiredEntity(entityClass);
            StatementMapper statementMapper = this.statementMapper().forType(entityClass);

            SqlIdentifier columnName = entity.hasIdProperty() ? entity.getRequiredIdProperty().getColumnName() : SqlIdentifier.unquoted("*");

            StatementMapper.SelectSpec selectSpec = statementMapper.createSelect(tableName)
                    .doWithTable((table, spec) -> spec.withProjection(columnName))
                    .limit(1);

            selectSpec = selectWithCriteria(selectSpec, query, entityClass);

            PreparedOperation<?> operation = statementMapper.getMappedObject(selectSpec);

            return this.databaseClient().sql(operation).map((r, md) -> r).first().hasElement();
        }

        private Mono<Long> doCount(Query query, Class<T> entityClass, SqlIdentifier tableName) {

            RelationalPersistentEntity<T> entity = R2dbcMappingKit.getRequiredEntity(entityClass);
            StatementMapper statementMapper = this.statementMapper().forType(entityClass);

            StatementMapper.SelectSpec selectSpec = statementMapper.createSelect(tableName).doWithTable((table, spec) -> {
                Expression countExpression = entity.hasIdProperty() ? table.column(entity.getRequiredIdProperty().getColumnName()) : Expressions.asterisk();
                return spec.withProjection(Functions.count(countExpression));
            });

            selectSpec = selectWithCriteria(selectSpec, query, entityClass);

            PreparedOperation<?> operation = statementMapper.getMappedObject(selectSpec);

            return this.databaseClient().sql(operation).map((r, md) -> r.get(0, Long.class)).first().defaultIfEmpty(0L);
        }

        private RowsFetchSpec<R> doSelect(Query query, Class<T> entityClass, SqlIdentifier tableName, Class<R> returnType) {

            // 是否是聚合对象
            boolean isAggregate = false;

            if (entityClass.isAnnotationPresent(TableEntity.class)) {
                isAggregate = entityClass.getAnnotation(TableEntity.class).aggregate();
            }

            StatementMapper statementMapper = isAggregate ? this.statementMapper() : this.statementMapper().forType(entityClass);

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

            selectSpec = selectWithCriteria(selectSpec, query, entityClass);

            PreparedOperation<?> operation = statementMapper.getMappedObject(selectSpec);

            return reactiveEntityTemplate.getRowsFetchSpec(this.databaseClient().sql(operation), entityClass, returnType);
        }

        private Mono<R> selectMono(Query query, Class<T> entityClass, SqlIdentifier tableName,
                                   Class<R> returnType, Function<RowsFetchSpec<R>, Mono<R>> resultHandler) {
            return resultHandler.apply(doSelect(query, entityClass, tableName, returnType))
                    .flatMap(result -> selectReference(entityClass, result))
                    .flatMap(it -> this.reactiveEntityTemplate.maybeCallAfterConvert(it, tableName));
        }

        private Flux<R> selectFlux(Query query, Class<T> entityClass, SqlIdentifier tableName,
                                   Class<R> returnType, Function<RowsFetchSpec<R>, Flux<R>> resultHandler) {
            return resultHandler.apply(doSelect(query, entityClass, tableName, returnType))
                    .flatMap(result -> selectReference(entityClass, result))
                    .flatMap(it -> this.reactiveEntityTemplate.maybeCallAfterConvert(it, tableName));
        }

        private Mono<R> selectReference(Class<T> entityClass, R result) {
            RelationalPersistentEntity<T> entity = R2dbcMappingKit.getRequiredEntity(entityClass);
            List<RelationalPersistentProperty> referenceProperties = R2dbcMappingKit.getReferenceProperties(entity);
            if (referenceProperties.isEmpty()) {
                return Mono.just(result);
            }
            return Flux.fromIterable(referenceProperties)
                    .map(property -> Reference.build(entity, property, result))
                    .filter(Reference::canReference)
                    .flatMap(reference -> reference.doSelect(reactiveEntityTemplate, result))
                    .last();
        }

        /**
         * 创建查询单元，加入了逻辑删除的判断
         */
        private static <T> StatementMapper.SelectSpec selectWithCriteria(StatementMapper.SelectSpec selectSpec, Query query, Class<T> entityClass) {
            Optional<CriteriaDefinition> criteriaOptional = query.getCriteria();
            // TODO: 记得来改这里
            if (R2dbcMappingKit.isLogicDeleteEnable(entityClass, false)) {
                criteriaOptional = query.getCriteria()
                        .or(() -> Optional.of(Criteria.empty()))
                        .map(criteriaDefinition -> {
                            // 获取查询对象中的逻辑删除字段和值，写入到criteria中
                            Pair<String, Object> logicDeleteColumn = R2dbcMappingKit.getLogicDeleteColumn(entityClass, R2dbcMappingKit.LogicDeleteValue.UNDELETE_VALUE);
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
            if (!query.getColumns().isEmpty()) {
                return query.getColumns().stream()
                        .map(table::column)
                        .map(Expression.class::cast)
                        .toList();
            }
            if (returnType.isInterface()) {
                ProjectionInformation projectionInformation = this.projectionFactory().getProjectionInformation(returnType);
                if (projectionInformation.isClosed()) {
                    return projectionInformation.getInputProperties().stream()
                            .map(FeatureDescriptor::getName)
                            .map(table::column)
                            .map(Expression.class::cast)
                            .toList();
                }
            }
            RelationalPersistentEntity<E> entity = R2dbcMappingKit.getRequiredEntity(entityClass);
            boolean isAggregateEntity = R2dbcMappingKit.isAggregateEntity(entityClass);
            return StreamUtils.createStreamFromIterator(entity.iterator())
                    .filter(R2dbcMappingKit::isPropertyExists)
                    .map(property -> isAggregateEntity ? createFunction(property) : createColumn(property, table))
                    .toList();
        }

        private Expression createColumn(RelationalPersistentProperty property, Table table) {
            Expression expression;
            if (property.isIdProperty()) {
                expression = table.column(property.getColumnName());
            } else {
                TableColumn tableColumn = property.getRequiredAnnotation(TableColumn.class);
                Table columnTable = tableColumn.fromTable().isEmpty() ? table : Table.create(tableColumn.fromTable());

                String columnName = property.getColumnName().getReference();
                boolean isColumnWithTable = columnName.contains(".");
                String alias = tableColumn.alias();
                if (alias.isEmpty()) {
                    if (isColumnWithTable) {
                        expression = Expressions.just(columnName);
                    } else {
                        expression = columnTable.column(property.getColumnName());
                    }
                } else {
                    if (isColumnWithTable) {
                        expression = Expressions.just(columnName + SQL_AS + alias);
                    } else {
                        expression = Column.aliased(columnName, columnTable, alias);
                    }
                }
            }
            return expression;
        }

        private Expression createFunction(RelationalPersistentProperty property) {
            // 聚合函数必须要使用Expressions.just()直接创建表达式
            // 实测使用Column创建的话，会被添加表名作为前缀，导致SQL的语法是错的
            TableColumn tableColumn = property.getRequiredAnnotation(TableColumn.class);
            if (!R2dbcMappingKit.isFunctionProperty(property)) {
                String sql = tableColumn.name();
                // 如果设置了别名，添加别名的语法
                if (!ObjectUtils.isEmpty(tableColumn.alias())) {
                    sql += SQL_AS + tableColumn.alias();
                }
                // 如果不是函数，直接创建标准表达式
                return Expressions.just(sql);
            } else {
                // 如果是函数，则采用函数的方式创建函数
                return SimpleFunction.create(tableColumn.function(), Collections.singletonList(Expressions.just(tableColumn.name())))
                        .as(tableColumn.alias());
            }
        }

        private record Reference(
                TableReference annotation,
                RelationalPersistentProperty property,
                Object referenceValue
        ) {

            private static <T, R> Reference build(RelationalPersistentEntity<T> entity, RelationalPersistentProperty property, R result) {
                TableReference tableReference = property.getRequiredAnnotation(TableReference.class);
                RelationalPersistentProperty referenceProperty;
                String keyColumn = tableReference.keyColumn();
                if (keyColumn.isEmpty()) {
                    referenceProperty = entity.getIdProperty();
                } else {
                    referenceProperty = entity.getPersistentProperty(keyColumn);
                }
                Assert.notNull(referenceProperty, "Property must not be null.");
                Assert.notNull(referenceProperty.getField(), "Field must not be null.");
                Object referenceValue = ReflectionKit.invokeGetter(referenceProperty.getRequiredGetter(), result);
                if (referenceValue instanceof String stringValue && !ObjectUtils.isEmpty(tableReference.delimiter())) {
                    referenceValue = stringValue.split(tableReference.delimiter());
                }
                return new Reference(tableReference, property, referenceValue);
            }

            private boolean canReference() {
                return !ObjectUtils.isEmpty(referenceValue());
            }

            private Mono<?> buildMono(ReactiveEntityTemplate reactiveEntityTemplate) {
                Criteria.CriteriaStep where = Criteria.where(annotation().referenceColumn());
                TableReference.ReferenceCondition referenceCondition = annotation().referenceCondition();
                if (referenceValue() instanceof Collection<?> || referenceValue() instanceof Object[]) {
                    referenceCondition = TableReference.ReferenceCondition.IN;
                }
                Criteria criteria = referenceCondition.getCondition().apply(where, referenceValue());
                R2dbcSelectOperation.TerminatingSelect<?> matching = reactiveEntityTemplate
                        .select(property().getActualType())
                        .matching(Query.query(criteria));
                Mono<?> mono;
                if (property().isCollectionLike()) {
                    mono = matching.all().collectList();
                } else {
                    mono = matching.one();
                }
                return mono;
            }

            private <R> Mono<R> doSelect(ReactiveEntityTemplate reactiveEntityTemplate, R result) {
                return buildMono(reactiveEntityTemplate)
                        .map(object -> {
                            Assert.notNull(property().getField(), "Field must not bet null");
                            Object value = object;
                            if (property().isArray() && object instanceof Collection<?> collection) {
                                value = CollectionKit.castCollectionToArray(collection, property().getActualType());
                            }
                            ReflectionKit.invokeSetter(property().getRequiredSetter(), result, value);
                            return result;
                        });
            }

        }

    }

}
