package com.jxpanda.r2dbc.spring.data.core.operation;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.extension.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.extension.annotation.TableEntity;
import io.r2dbc.postgresql.util.Assert;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.mapping.callback.ReactiveEntityCallbacks;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.ProjectionInformation;
import org.springframework.data.r2dbc.convert.EntityRowMapper;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.r2dbc.mapping.event.AfterConvertCallback;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.*;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.RowsFetchSpec;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.beans.FeatureDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class ReactiveOperationSupport {

    private static final String SQL_AS = " AS ";

    protected final ReactiveEntityTemplate template;

    public ReactiveOperationSupport(ReactiveEntityTemplate template) {
        this.template = template;
    }


    @SuppressWarnings("unchecked")

    protected static class ReactiveSupport<T, R> {

        /**
         * entityTemplate
         */
        private final ReactiveEntityTemplate template;

        /**
         * 领域对象类型，通常是实体对象的类型
         */
        private final Class<T> domainType;

        /**
         * 返回值类型
         */
        private final Class<R> returnType;

        /**
         * 查询条件对象
         */
        private final Query query;

        /**
         * 表名
         */
        private final SqlIdentifier tableName;


        protected ReactiveSupport(ReactiveEntityTemplate template, Class<T> domainType, Class<R> returnType) {
            this(template, domainType, returnType, null, null);
        }


        protected ReactiveSupport(ReactiveEntityTemplate template, Class<T> domainType, Class<R> returnType, @Nullable Query query, @Nullable SqlIdentifier tableName) {
            this.template = template;
            this.domainType = domainType;
            this.returnType = returnType;
            this.query = query == null ? Query.empty() : query;
            this.tableName = tableName == null ? getTableName(domainType) : tableName;
        }


        public MappingContext<RelationalPersistentEntity<T>, ? extends RelationalPersistentProperty> getMappingContext() {
            return (MappingContext<RelationalPersistentEntity<T>, ? extends RelationalPersistentProperty>) template.getMappingContext();
        }

        protected ReactiveEntityTemplate getTemplate() {
            return template;
        }

        protected Class<T> getDomainType() {
            return domainType;
        }

        protected Class<R> getReturnType() {
            return returnType;
        }

        protected Query getQuery() {
            return query;
        }

        protected SqlIdentifier getTableName() {
            return tableName;
        }

        protected StatementMapper getStatementMapper() {
            return template.getDataAccessStrategy().getStatementMapper();
        }

        public DatabaseClient getDatabaseClient() {
            return template.getDatabaseClient();
        }

        public SqlIdentifier getTableName(Class<T> entityClass) {
            return getRequiredEntity(entityClass).getTableName();
        }

        @Nullable
        protected RelationalPersistentEntity<T> getPersistentEntity(Class<T> entityClass) {
            return this.getMappingContext().getPersistentEntity(entityClass);
        }

        protected RelationalPersistentEntity<T> getRequiredEntity(Class<T> entityClass) {
            return this.getMappingContext().getRequiredPersistentEntity(entityClass);
        }

        private SqlIdentifier getTableNameOrEmpty(Class<T> entityClass) {

            RelationalPersistentEntity<T> entity = getPersistentEntity(entityClass);

            return entity != null ? entity.getTableName() : SqlIdentifier.EMPTY;
        }

        protected boolean isQueryEntity(Class<T> entityClass) {
            RelationalPersistentEntity<T> persistentEntity = getPersistentEntity(entityClass);
            return persistentEntity != null && isQueryEntity(persistentEntity);
        }

        protected boolean isQueryEntity(RelationalPersistentEntity<T> relationalPersistentEntity) {
            TableEntity tableEntity = relationalPersistentEntity.findAnnotation(TableEntity.class);
            return tableEntity != null && tableEntity.isQuery();
        }

        protected boolean isPropertyExists(RelationalPersistentProperty property) {
            TableColumn tableColumn = property.findAnnotation(TableColumn.class);
            return property.isIdProperty() || (tableColumn != null && tableColumn.exists());
        }

        protected boolean isFunctionProperty(RelationalPersistentProperty property) {
            TableColumn tableColumn = property.findAnnotation(TableColumn.class);
            return tableColumn != null && !ObjectUtils.isEmpty(tableColumn.function());
        }

        protected <E> Mono<E> maybeCallAfterConvert(E object, SqlIdentifier table) {

            ReactiveEntityCallbacks entityCallbacks = template.getEntityCallbacks();

            if (entityCallbacks != null) {
                return entityCallbacks.callback(AfterConvertCallback.class, object, table);
            }

            return Mono.just(object);
        }

        protected <E> BiFunction<Row, RowMetadata, E> getRowMapper(Class<E> typeToRead) {
            return new EntityRowMapper<>(typeToRead, template.getConverter());
        }

        protected List<Expression> getSelectProjection(Table table, Query query, Class<T> entityClass, Class<R> returnType) {

            if (query.getColumns().isEmpty()) {

                if (returnType.isInterface()) {

                    ProjectionInformation projectionInformation = template.getProjectionFactory().getProjectionInformation(returnType);

                    if (projectionInformation.isClosed()) {
                        return projectionInformation.getInputProperties().stream().map(FeatureDescriptor::getName).map(table::column)
                                .collect(Collectors.toList());
                    }
                }

                RelationalPersistentEntity<T> entity = getRequiredEntity(entityClass);

                List<Expression> columns = new ArrayList<>();

                boolean isQueryEntity = isQueryEntity(entityClass);

                entity.forEach(property -> {
                    if (isPropertyExists(property)) {
                        Expression expression;
                        if (!isQueryEntity) {
                            expression = table.column(property.getColumnName());
                        } else {
                            TableColumn tableColumn = property.findAnnotation(TableColumn.class);
                            Assert.isTrue(tableColumn != null, "");
                            if (!isFunctionProperty(property)) {
                                String sql = tableColumn.name();
                                if (!ObjectUtils.isEmpty(tableColumn.alias())) {
                                    sql += SQL_AS + tableColumn.alias();
                                }
                                // 如果不是函数，直接创建标准表达式
                                expression = Expressions.just(sql);
                            } else {
                                // 如果是函数，则采用函数的方式创建函数
                                expression = SimpleFunction.create(tableColumn.function(), Collections.singletonList(Expressions.just(tableColumn.name())))
                                        .as(tableColumn.alias());
                            }
                        }
                        columns.add(expression);
                    }
                });
                return columns;
            }

            return query.getColumns().stream().map(table::column).collect(Collectors.toList());
        }


        protected RowsFetchSpec<R> getRowsFetchSpec(DatabaseClient.GenericExecuteSpec executeSpec, Class<T> entityClass,
                                                    Class<R> returnType) {

            boolean simpleType;

            BiFunction<Row, RowMetadata, R> rowMapper;
            if (returnType.isInterface()) {
                simpleType = this.template.getConverter().isSimpleType(entityClass);
                rowMapper = getRowMapper(entityClass)
                        .andThen(source -> template.getProjectionFactory().createProjection(returnType, source));
            } else {
                simpleType = this.template.getConverter().isSimpleType(returnType);
                rowMapper = getRowMapper(returnType);
            }

            // avoid top-level null values if the read type is a simple one (e.g. SELECT MAX(age) via Integer.class)
            if (simpleType) {
                return new UnwrapOptionalFetchSpecAdapter<>(
                        executeSpec.map((row, metadata) -> Optional.ofNullable(rowMapper.apply(row, metadata))));
            }

            return executeSpec.map(rowMapper);
        }

        private record UnwrapOptionalFetchSpecAdapter<T>(
                RowsFetchSpec<Optional<T>> delegate) implements RowsFetchSpec<T> {

            @NotNull
            @Override
            public Mono<T> one() {
                return delegate.one().handle((optional, sink) -> optional.ifPresent(sink::next));
            }

            @NotNull
            @Override
            public Mono<T> first() {
                return delegate.first().handle((optional, sink) -> optional.ifPresent(sink::next));
            }

            @NotNull
            @Override
            public Flux<T> all() {
                return delegate.all().handle((optional, sink) -> optional.ifPresent(sink::next));
            }
        }

    }
}


