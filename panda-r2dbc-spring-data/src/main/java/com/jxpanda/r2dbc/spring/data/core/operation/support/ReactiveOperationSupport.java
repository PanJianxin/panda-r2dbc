package com.jxpanda.r2dbc.spring.data.core.operation.support;

import com.jxpanda.r2dbc.spring.data.convert.EntityRowMapper;
import com.jxpanda.r2dbc.spring.data.convert.R2dbcConverter;
import com.jxpanda.r2dbc.spring.data.core.R2dbcEntityTemplate;
import com.jxpanda.r2dbc.spring.data.core.StatementMapper;
import com.jxpanda.r2dbc.spring.data.mapping.event.AfterConvertCallback;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.mapping.callback.ReactiveEntityCallbacks;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.ProjectionInformation;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.RowsFetchSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.beans.FeatureDescriptor;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class ReactiveOperationSupport {

    protected final R2dbcEntityTemplate template;

    public ReactiveOperationSupport(R2dbcEntityTemplate template) {
        this.template = template;
    }


    @SuppressWarnings("unchecked")

    protected static class ReactiveSupport<T, R> {

        /**
         * entityTemplate
         */
        private final R2dbcEntityTemplate template;

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


//        private StatementMapper statementMapper;

//        private DatabaseClient databaseClient;

//        private R2dbcConverter converter;

//        private final MappingContext<RelationalPersistentEntity<T>, ? extends RelationalPersistentProperty> mappingContext;

//        private @Nullable ReactiveEntityCallbacks entityCallbacks;

//        private final SpelAwareProxyProjectionFactory projectionFactory;

        protected ReactiveSupport(R2dbcEntityTemplate template, Class<T> domainType, Class<R> returnType) {
            this(template, domainType, returnType, null, null);
        }


        protected ReactiveSupport(R2dbcEntityTemplate template, Class<T> domainType, Class<R> returnType, @Nullable Query query, @Nullable SqlIdentifier tableName) {
            this.template = template;
            this.domainType = domainType;
            this.returnType = returnType;
            this.query = query == null ? Query.empty() : query;
            this.tableName = tableName == null ? getTableName(domainType) : tableName;
        }


        public MappingContext<RelationalPersistentEntity<T>, ? extends RelationalPersistentProperty> getMappingContext() {
            return (MappingContext<RelationalPersistentEntity<T>, ? extends RelationalPersistentProperty>) template.getMappingContext();
        }

        protected R2dbcEntityTemplate getTemplate() {
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
            SqlIdentifier name = getRequiredEntity(entityClass).getTableName();
            return name;
        }

        @Nullable
        protected RelationalPersistentEntity<T> getPersistentEntity(Class<T> entityClass){
            return this.getMappingContext().getPersistentEntity(entityClass);
        }

        protected RelationalPersistentEntity<T> getRequiredEntity(Class<T> entityClass) {
            return this.getMappingContext().getRequiredPersistentEntity(entityClass);
        }

        private SqlIdentifier getTableNameOrEmpty(Class<T> entityClass) {

            RelationalPersistentEntity<T> entity = getPersistentEntity(entityClass);

            return entity != null ? entity.getTableName() : SqlIdentifier.EMPTY;
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

        protected List<Expression> getSelectProjection(Table table, Query query, Class<R> returnType) {

            if (query.getColumns().isEmpty()) {

                if (returnType.isInterface()) {

                    ProjectionInformation projectionInformation = template.getProjectionFactory().getProjectionInformation(returnType);

                    if (projectionInformation.isClosed()) {
                        return projectionInformation.getInputProperties().stream().map(FeatureDescriptor::getName).map(table::column)
                                .collect(Collectors.toList());
                    }
                }

                return Collections.singletonList(table.asterisk());
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


