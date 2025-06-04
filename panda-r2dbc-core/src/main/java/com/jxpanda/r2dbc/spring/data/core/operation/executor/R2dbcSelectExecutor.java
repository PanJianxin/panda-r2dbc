package com.jxpanda.r2dbc.spring.data.core.operation.executor;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableReference;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model.R2dbcPluginContext;
import com.jxpanda.r2dbc.spring.data.core.kit.R2dbcMappingKit;
import com.jxpanda.r2dbc.spring.data.core.operation.contract.R2dbcSelectOperation;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.CollectionKit;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.ReflectionKit;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.reactivestreams.Publisher;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.projection.EntityProjection;
import org.springframework.data.projection.ProjectionInformation;
import org.springframework.data.r2dbc.convert.EntityRowMapper;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.relational.core.conversion.AbstractRelationalConverter;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.*;
import org.springframework.data.relational.domain.RowDocument;
import org.springframework.data.util.StreamUtils;
import org.springframework.lang.NonNull;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.r2dbc.core.RowsFetchSpec;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.beans.FeatureDescriptor;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author Panda
 */
public class R2dbcSelectExecutor<T, R> extends R2dbcOperationExecutor.ReadExecutor<T, R> {

    private static final String SQL_AS = " AS ";

    private final Function<R2dbcOperationContext<T, R>, StatementMapper.SelectSpec> specBuilder;

    private final Function<R2dbcOperationContext<T, R>, BiFunction<Row, RowMetadata, R>> rowMapperBuilder;

    private R2dbcSelectExecutor(R2dbcOperationContext<T, R> operationContext,
                                Function<R2dbcOperationContext<T, R>, Query> queryHandler,
                                Function<R2dbcOperationContext<T, R>, StatementMapper.SelectSpec> specBuilder,
                                Function<R2dbcOperationContext<T, R>, BiFunction<Row, RowMetadata, R>> rowMapperBuilder) {
        super(operationContext, queryHandler);
        this.specBuilder = specBuilder != null ? specBuilder : defaultSpecBuilder();
        this.rowMapperBuilder = rowMapperBuilder != null ? rowMapperBuilder : defaultRowMapperBuilder();
    }

    public static <T, R> R2dbcSelectExecutorBuilder<T, R> builder() {
        return new R2dbcSelectExecutorBuilder<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <P extends Publisher<R>> P fetch(R2dbcOperationContext<T, R> operationContext, Function<RowsFetchSpec<R>, P> resultHandler) {
        P publisher = resultHandler.apply(new FetchSpecTypeReference<>());
        if (publisher instanceof Mono<?>) {
            return (P) doFetchMono(operationContext, (Function<RowsFetchSpec<R>, Mono<R>>) resultHandler);
        } else if (publisher instanceof Flux<?>) {
            return (P) doFetchFlux(operationContext, (Function<RowsFetchSpec<R>, Flux<R>>) resultHandler);
        }
        return publisher;
    }

    private Mono<R> doFetchMono(R2dbcOperationContext<T, R> operationContext, Function<RowsFetchSpec<R>, Mono<R>> resultHandler) {
        return beforeFetch(operationContext)
                .flatMap(tuple -> resultHandler.apply(tuple.getT1())
                        .map(result -> tuple.getT2().withResult(this, result)))
                .flatMap(pluginContext -> afterFetch(operationContext, pluginContext));

    }

    private Flux<R> doFetchFlux(R2dbcOperationContext<T, R> operationContext, Function<RowsFetchSpec<R>, Flux<R>> resultHandler) {
        return beforeFetch(operationContext)
                .flatMapMany(tuple -> resultHandler.apply(tuple.getT1())
                        .map(result -> tuple.getT2().withResult(this, result)))
                .flatMap(pluginContext -> afterFetch(operationContext, pluginContext));

    }

    private Mono<R> afterFetch(R2dbcOperationContext<T, R> operationContext, R2dbcPluginContext<T, R> pluginContext) {
        return selectReference(operationContext, pluginContext.getResult())
                .flatMap(result -> template().maybeCallAfterConvert(result, operationContext.getTableName()))
                .flatMap(result -> pluginExecutor().runAfter(pluginContext.withResult(this, result)))
                .mapNotNull(R2dbcPluginContext::getResult)
                .doOnSuccess(operationContext::withResult);
    }

    private Mono<Tuple2<RowsFetchSpec<R>, R2dbcPluginContext<T, R>>> beforeFetch(R2dbcOperationContext<T, R> operationContext) {
        return pluginExecutor().runBefore(operationContext.createPluginContext(this))
                .flatMap(pluginContext -> {
                    StatementMapper.SelectSpec selectSpec = specBuilder.apply(operationContext);
                    if (pluginContext.getCriteria() != null) {
                        selectSpec = selectSpec.withCriteria(pluginContext.getCriteria());
                    }

                    PreparedOperation<?> preparedOperation = operationContext.getStatementMapper().getMappedObject(selectSpec);

                    DatabaseClient.GenericExecuteSpec executeSpec = databaseClient().sql(preparedOperation)
                            .filter(template().getStatementFilterFunction()
                                    .andThen(operationContext.getFilterFunction()));

                    RowsFetchSpec<R> rowsFetchSpec;

                    if (operationContext.isSimpleResultType()) {
                        rowsFetchSpec = new UnwrapOptionalFetchSpecAdapter<>(executeSpec
                                .map((row, metadata) -> Optional.ofNullable(rowMapperBuilder.apply(operationContext).apply(row, metadata))));
                    } else {
                        rowsFetchSpec = executeSpec.map(rowMapperBuilder.apply(operationContext));
                    }
                    return Mono.just(rowsFetchSpec)
                            .zipWith(Mono.just(pluginContext));
                });
    }


    private Function<R2dbcOperationContext<T, R>, StatementMapper.SelectSpec> defaultSpecBuilder() {
        return operationContext -> {
            Query query = operationContext.getQuery();
            StatementMapper.SelectSpec selectSpec = operationContext.getStatementMapper().createSelect(operationContext.getTableName())
                    .doWithTable((table, spec) -> spec.withProjection(getSelectProjection(table, query, operationContext.getEntityType(), operationContext.getResultType())));
            if (query.getLimit() > 0) {
                selectSpec = selectSpec.limit(query.getLimit());
            }
            if (query.getOffset() > 0) {
                selectSpec = selectSpec.offset(query.getOffset());
            }
            if (query.isSorted()) {
                selectSpec = selectSpec.withSort(query.getSort());
            }
            return selectSpec;
        };
    }

    @SuppressWarnings({"unchecked"})
    private Function<R2dbcOperationContext<T, R>, BiFunction<Row, RowMetadata, R>> defaultRowMapperBuilder() {
        return operationContext -> {
            Class<R> resultType = operationContext.getResultType();
            Class<T> entityType = operationContext.getEntityType();
            boolean simpleType = operationContext.isSimpleResultType();

            BiFunction<Row, RowMetadata, R> rowMapper;

            // Bridge-code: Consider Converter<Row, T> until we have fully migrated to RowDocument
            if (converter() instanceof AbstractRelationalConverter relationalConverter
                && relationalConverter.getConversions().hasCustomReadTarget(Row.class, resultType)) {
                ConversionService conversionService = relationalConverter.getConversionService();
                rowMapper = (row, rowMetadata) -> (R) conversionService.convert(row, resultType);
            } else if (simpleType) {
                rowMapper = new EntityRowMapper<>(resultType, converter());
            } else {
                EntityProjection<R, T> projection = converter().introspectProjection(resultType, entityType);
                Class<R> typeToRead = projection.isProjection() ? resultType
                        : resultType.isInterface() ? (Class<R>) entityType : resultType;

                rowMapper = (row, rowMetadata) -> {
                    RowDocument document = dataAccessStrategy().toRowDocument(typeToRead, row, rowMetadata.getColumnMetadatas());
                    return converter().project(projection, document);
                };
            }
            return rowMapper;
        };

    }


    private Mono<R> selectReference(R2dbcOperationContext<T, R> operationContext, R result) {
        RelationalPersistentEntity<T> entity = operationContext.getRelationalPersistentEntity();
        List<RelationalPersistentProperty> referenceProperties = R2dbcMappingKit.getReferenceProperties(entity);
        if (referenceProperties.isEmpty()) {
            return Mono.just(result);
        }
        return Flux.fromIterable(referenceProperties)
                .map(property -> Reference.build(entity, property, result))
                .filter(Reference::canReference)
                .flatMap(reference -> reference.doSelect(template(), result))
                .last();
    }


    private <E, RT> List<Expression> getSelectProjection(Table table, Query query, Class<E> entityClass, Class<RT> resultType) {
        if (!query.getColumns().isEmpty()) {
            return query.getColumns().stream()
                    .map(table::column)
                    .map(Expression.class::cast)
                    .toList();
        }
        if (resultType.isInterface()) {
            ProjectionInformation projectionInformation = this.projectionFactory().getProjectionInformation(resultType);
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
                .filter(property -> R2dbcMappingKit.isPropertyExists(entity, property))
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
        // 别名
        String alias = tableColumn.alias();
        if (R2dbcMappingKit.isFunctionProperty(property)) {
            Assert.isTrue(!ObjectUtils.isEmpty(alias), "Alias must not be null with function property");
            // 如果是函数，则采用函数的方式创建函数
            return SimpleFunction.create(tableColumn.function(), Collections.singletonList(Expressions.just(tableColumn.name())))
                    .as(alias);
        } else {
            String sql = tableColumn.name();
            // 如果设置了别名，添加别名的语法
            if (!ObjectUtils.isEmpty(alias)) {
                sql += SQL_AS + alias;
            }
            // 如果不是函数，直接创建标准表达式
            return Expressions.just(sql);
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

    private record FetchSpecTypeReference<T>() implements RowsFetchSpec<T> {


        @NonNull
        @Override
        public Mono<T> one() {
            return Mono.empty();
        }

        @NonNull
        @Override
        public Mono<T> first() {
            return Mono.empty();
        }

        @NonNull
        @Override
        public Flux<T> all() {
            return Flux.empty();
        }
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

    public static final class R2dbcSelectExecutorBuilder<T, R> extends R2dbcOperationExecutor.R2dbcExecutorBuilder<T, R, R2dbcSelectExecutor<T, R>, R2dbcSelectExecutorBuilder<T, R>> {
        private Function<R2dbcOperationContext<T, R>, StatementMapper.SelectSpec> specBuilder;

        private Function<R2dbcOperationContext<T, R>, BiFunction<Row, RowMetadata, R>> rowMapperBuilder;

        public R2dbcSelectExecutorBuilder<T, R> specBuilder(Function<R2dbcOperationContext<T, R>, StatementMapper.SelectSpec> specBuilder) {
            this.specBuilder = specBuilder;
            return this;
        }


        public R2dbcSelectExecutorBuilder<T, R> rowMapperBuilder(Function<R2dbcOperationContext<T, R>, BiFunction<Row, RowMetadata, R>> rowMapperBuilder) {
            this.rowMapperBuilder = rowMapperBuilder;
            return this;
        }

        public R2dbcSelectExecutor<T, R> buildExecutor() {
            return new R2dbcSelectExecutor<>(operationContext, queryHandler, specBuilder, rowMapperBuilder);
        }

        @Override
        protected R2dbcSelectExecutorBuilder<T, R> self() {
            return this;
        }

    }
}