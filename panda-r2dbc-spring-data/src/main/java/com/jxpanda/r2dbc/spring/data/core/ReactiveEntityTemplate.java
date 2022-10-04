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

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.reactivestreams.Publisher;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.ProjectionInformation;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.core.ReactiveSelectOperationAdapter;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.util.ProxyUtils;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.r2dbc.core.RowsFetchSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.beans.FeatureDescriptor;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class ReactiveEntityTemplate extends R2dbcEntityTemplate {


    private final SpelAwareProxyProjectionFactory projectionFactory;

    public ReactiveEntityTemplate(ConnectionFactory connectionFactory) {
        super(connectionFactory);
        this.projectionFactory = new SpelAwareProxyProjectionFactory();
    }

    public ReactiveEntityTemplate(DatabaseClient databaseClient, R2dbcDialect dialect) {
        super(databaseClient, dialect);
        this.projectionFactory = new SpelAwareProxyProjectionFactory();
    }

    public ReactiveEntityTemplate(DatabaseClient databaseClient, R2dbcDialect dialect, R2dbcConverter converter) {
        super(databaseClient, dialect, converter);
        this.projectionFactory = new SpelAwareProxyProjectionFactory();
    }

    public ReactiveEntityTemplate(DatabaseClient databaseClient, ReactiveDataAccessStrategy strategy) {
        super(databaseClient, strategy);
        this.projectionFactory = new SpelAwareProxyProjectionFactory();
    }

    // -------------------------------------------------------------------------
    // Methods dealing with org.springframework.data.r2dbc.core.FluentR2dbcOperations
    // -------------------------------------------------------------------------


    SqlIdentifier getTableName(Class<?> entityClass) {
        return getRequiredEntity(entityClass).getTableName();
    }

    private MappingContext<? extends RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> getMappingContext() {
        return this.getDataAccessStrategy().getConverter().getMappingContext();
    }

    SqlIdentifier getTableNameOrEmpty(Class<?> entityClass) {

        RelationalPersistentEntity<?> entity = getMappingContext().getPersistentEntity(entityClass);

        return entity != null ? entity.getTableName() : SqlIdentifier.EMPTY;
    }

    private RelationalPersistentEntity<?> getRequiredEntity(Class<?> entityClass) {
        return getMappingContext().getRequiredPersistentEntity(entityClass);
    }

    private <T> RelationalPersistentEntity<T> getRequiredEntity(T entity) {
        Class<?> entityType = ProxyUtils.getUserClass(entity);
        return (RelationalPersistentEntity<T>) getRequiredEntity(entityType);
    }

    @Override
    public <T> ReactiveSelect<T> select(Class<T> domainType) {
        return new ReactiveSelectOperationAdapter(this).select(domainType);
    }


    @SuppressWarnings("unchecked")
    <T, P extends Publisher<T>> P doSelect(Query query, Class<?> entityClass, SqlIdentifier tableName,
                                           Class<T> returnType, Function<RowsFetchSpec<T>, P> resultHandler) {

        RowsFetchSpec<T> fetchSpec = doSelect(query, entityClass, tableName, returnType);

        P result = resultHandler.apply(fetchSpec);

        if (result instanceof Mono) {
            return (P) ((Mono<?>) result).flatMap(it -> maybeCallAfterConvert(it, tableName));
        }

        return (P) ((Flux<?>) result).flatMap(it -> maybeCallAfterConvert(it, tableName));
    }

    private <T> RowsFetchSpec<T> doSelect(Query query, Class<?> entityClass, SqlIdentifier tableName,
                                          Class<T> returnType) {

        StatementMapper statementMapper = getDataAccessStrategy().getStatementMapper().forType(entityClass);

        StatementMapper.SelectSpec selectSpec = statementMapper //
                .createSelect(tableName) //
                .doWithTable((table, spec) -> spec.withProjection(getSelectProjection(table, query, returnType)));

        if (query.getLimit() > 0) {
            selectSpec = selectSpec.limit(query.getLimit());
        }

        if (query.getOffset() > 0) {
            selectSpec = selectSpec.offset(query.getOffset());
        }

        if (query.isSorted()) {
            selectSpec = selectSpec.withSort(query.getSort());
        }

        Optional<CriteriaDefinition> criteria = query.getCriteria();
        if (criteria.isPresent()) {
            selectSpec = criteria.map(selectSpec::withCriteria).orElse(selectSpec);
        }

        PreparedOperation<?> operation = statementMapper.getMappedObject(selectSpec);

        return getRowsFetchSpec(getDatabaseClient().sql(operation), entityClass, returnType);
    }

    private <T> List<Expression> getSelectProjection(Table table, Query query, Class<T> returnType) {

        if (query.getColumns().isEmpty()) {

            if (returnType.isInterface()) {

                ProjectionInformation projectionInformation = projectionFactory.getProjectionInformation(returnType);

                if (projectionInformation.isClosed()) {
                    return projectionInformation.getInputProperties().stream().map(FeatureDescriptor::getName).map(table::column)
                            .collect(Collectors.toList());
                }
            }

            return Collections.singletonList(table.asterisk());
        }

        return query.getColumns().stream().map(table::column).collect(Collectors.toList());
    }

    private <T> RowsFetchSpec<T> getRowsFetchSpec(DatabaseClient.GenericExecuteSpec executeSpec, Class<?> entityClass,
                                                  Class<T> returnType) {

        boolean simpleType;

        BiFunction<Row, RowMetadata, T> rowMapper;
        if (returnType.isInterface()) {
            simpleType = getConverter().isSimpleType(entityClass);
            rowMapper = getDataAccessStrategy().getRowMapper(entityClass)
                    .andThen(o -> projectionFactory.createProjection(returnType, o));
        } else {
            simpleType = getConverter().isSimpleType(returnType);
            rowMapper = getDataAccessStrategy().getRowMapper(returnType);
        }

        // avoid top-level null values if the read type is a simple one (e.g. SELECT MAX(age) via Integer.class)
        if (simpleType) {
            return new UnwrapOptionalFetchSpecAdapter<>(
                    executeSpec.map((row, metadata) -> Optional.ofNullable(rowMapper.apply(row, metadata))));
        }

        return executeSpec.map(rowMapper);
    }


    private record UnwrapOptionalFetchSpecAdapter<T>(RowsFetchSpec<Optional<T>> delegate) implements RowsFetchSpec<T> {

        @Override
            public Mono<T> one() {
                return delegate.one().handle((optional, sink) -> optional.ifPresent(sink::next));
            }

            @Override
            public Mono<T> first() {
                return delegate.first().handle((optional, sink) -> optional.ifPresent(sink::next));
            }

            @Override
            public Flux<T> all() {
                return delegate.all().handle((optional, sink) -> optional.ifPresent(sink::next));
            }
        }

}
