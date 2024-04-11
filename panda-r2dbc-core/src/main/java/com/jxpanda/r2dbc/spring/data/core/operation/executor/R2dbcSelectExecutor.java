package com.jxpanda.r2dbc.spring.data.core.operation.executor;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableReference;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcPluginContext;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcPluginName;
import com.jxpanda.r2dbc.spring.data.core.kit.R2dbcMappingKit;
import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcSelectOperation;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.CollectionKit;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.ReflectionKit;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.StringKit;
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
import org.springframework.data.relational.core.query.CriteriaDefinition;
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

    private final Function<R2dbcOperationParameter<T, R>, StatementMapper.SelectSpec> specBuilder;

    private final BiFunction<R2dbcOperationParameter<T, R>, StatementMapper.SelectSpec, Optional<CriteriaDefinition>> criteriaHandler;

    private final BiFunction<R2dbcOperationParameter<T, R>, StatementMapper.SelectSpec, PreparedOperation<?>> preparedOperationBuilder;

    private final Function<R2dbcOperationParameter<T, R>, BiFunction<Row, RowMetadata, R>> rowMapperBuilder;

    private R2dbcSelectExecutor(R2dbcOperationParameter<T, R> operationParameter,
                                Function<R2dbcOperationParameter<T, R>, Query> queryHandler,
                                Function<R2dbcOperationParameter<T, R>, StatementMapper.SelectSpec> specBuilder,
                                BiFunction<R2dbcOperationParameter<T, R>, StatementMapper.SelectSpec, Optional<CriteriaDefinition>> criteriaHandler,
                                BiFunction<R2dbcOperationParameter<T, R>, StatementMapper.SelectSpec, PreparedOperation<?>> preparedOperationBuilder,
                                Function<R2dbcOperationParameter<T, R>, BiFunction<Row, RowMetadata, R>> rowMapperBuilder) {
        super(operationParameter, queryHandler);
        this.specBuilder = specBuilder != null ? specBuilder : defaultSpecBuilder();
        this.criteriaHandler = criteriaHandler != null ? criteriaHandler : defaultCriteriaHandler();
        this.preparedOperationBuilder = preparedOperationBuilder != null ? preparedOperationBuilder : defaultPreparedOperationBuilder();
        this.rowMapperBuilder = rowMapperBuilder != null ? rowMapperBuilder : defaultRowMapperBuilder();
    }

    private BiFunction<R2dbcOperationParameter<T, R>, StatementMapper.SelectSpec, PreparedOperation<?>> defaultPreparedOperationBuilder() {
        return (parameter, selectSpec) -> parameter.getStatementMapper().getMappedObject(selectSpec);
    }

    public static <T, R> R2dbcSelectExecutorBuilder<T, R> builder() {
        return new R2dbcSelectExecutorBuilder<>();
    }

    // TODO: 这个函数的结构希望能重新组织一下，现在有点乱
    @Override
    protected <P extends Publisher<R>> P fetch(R2dbcOperationParameter<T, R> parameter, Function<RowsFetchSpec<R>, P> resultHandler) {

        StatementMapper.SelectSpec selectSpec = specBuilder.apply(parameter);
        Optional<CriteriaDefinition> criteriaDefinitionOptional = criteriaHandler.apply(parameter, selectSpec);
        R2dbcPluginContext<T, R, CriteriaDefinition> pluginContext = parameter.createPluginContext(R2dbcPluginName.LOGIC_DELETE, CriteriaDefinition.class, criteriaDefinitionOptional.orElse(Criteria.empty()));
        selectSpec = pluginExecutor().run(pluginContext).takeResult()
                .map(selectSpec::withCriteria)
                .orElse(selectSpec);

        PreparedOperation<?> preparedOperation = preparedOperationBuilder.apply(parameter, selectSpec);

        DatabaseClient.GenericExecuteSpec executeSpec = databaseClient().sql(preparedOperation);

        RowsFetchSpec<R> rowsFetchSpec;

        // avoid top-level null values if the read type is a simple one (e.g. SELECT MAX(age) via Integer.class)
        if (parameter.isSimpleReturnType()) {
            rowsFetchSpec = new UnwrapOptionalFetchSpecAdapter<>(executeSpec
                    .map((row, metadata) -> Optional.ofNullable(rowMapperBuilder.apply(parameter).apply(row, metadata))));
        } else {
            rowsFetchSpec = executeSpec.map(rowMapperBuilder.apply(parameter));
        }

        // 处理回调
        return callback(resultHandler.apply(rowsFetchSpec), parameter);
    }

    @SuppressWarnings("unchecked")
    private <P extends Publisher<R>> P callback(P publisher, R2dbcOperationParameter<T, R> parameter) {
        if (publisher instanceof Mono<?> mono) {
            return (P) mono.flatMap(result -> selectReference(parameter, (R) result))
                    .flatMap(it -> template().maybeCallAfterConvert(it, parameter.getTableName()));

        } else if (publisher instanceof Flux<?> flux) {
            return (P) flux.flatMap(result -> selectReference(parameter, (R) result))
                    .flatMap(it -> template().maybeCallAfterConvert(it, parameter.getTableName()));
        }
        return publisher;
    }

    private Function<R2dbcOperationParameter<T, R>, StatementMapper.SelectSpec> defaultSpecBuilder() {
        return parameter -> {
            Query query = parameter.getQuery();
            StatementMapper.SelectSpec selectSpec = parameter.getStatementMapper().createSelect(parameter.getTableName())
                    .doWithTable((table, spec) -> spec.withProjection(getSelectProjection(table, query, parameter.getDomainType(), parameter.getReturnType())));
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
    private Function<R2dbcOperationParameter<T, R>, BiFunction<Row, RowMetadata, R>> defaultRowMapperBuilder() {
        return parameter -> {
            Class<R> returnType = parameter.getReturnType();
            Class<T> domainType = parameter.getDomainType();
            boolean simpleType = parameter.isSimpleReturnType();

            BiFunction<Row, RowMetadata, R> rowMapper;

            // Bridge-code: Consider Converter<Row, T> until we have fully migrated to RowDocument
            if (converter() instanceof AbstractRelationalConverter relationalConverter
                && relationalConverter.getConversions().hasCustomReadTarget(Row.class, returnType)) {
                ConversionService conversionService = relationalConverter.getConversionService();
                rowMapper = (row, rowMetadata) -> (R) conversionService.convert(row, returnType);
            } else if (simpleType) {
                rowMapper = new EntityRowMapper<>(returnType, converter());
            } else {
                EntityProjection<R, T> projection = converter().introspectProjection(returnType, domainType);
                Class<R> typeToRead = projection.isProjection() ? returnType
                        : returnType.isInterface() ? (Class<R>) domainType : returnType;

                rowMapper = (row, rowMetadata) -> {
                    RowDocument document = dataAccessStrategy().toRowDocument(typeToRead, row, rowMetadata.getColumnMetadatas());
                    return converter().project(projection, document);
                };
            }
            return rowMapper;
        };

    }

    /**
     * 创建查询单元，加入了逻辑删除的判断
     */
    private BiFunction<R2dbcOperationParameter<T, R>, StatementMapper.SelectSpec, Optional<CriteriaDefinition>> defaultCriteriaHandler() {
        return ((operationParameter, selectSpec) -> operationParameter.getQuery().getCriteria());
    }


    private Mono<R> selectReference(R2dbcOperationParameter<T, R> parameter, R result) {
        RelationalPersistentEntity<T> entity = parameter.getRelationalPersistentEntity();
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
        private Function<R2dbcOperationParameter<T, R>, StatementMapper.SelectSpec> specBuilder;

        private BiFunction<R2dbcOperationParameter<T, R>, StatementMapper.SelectSpec, Optional<CriteriaDefinition>> criteriaHandler;

        private BiFunction<R2dbcOperationParameter<T, R>, StatementMapper.SelectSpec, PreparedOperation<?>> preparedOperationBuilder;

        private Function<R2dbcOperationParameter<T, R>, BiFunction<Row, RowMetadata, R>> rowMapperBuilder;

        public R2dbcSelectExecutorBuilder<T, R> specBuilder(Function<R2dbcOperationParameter<T, R>, StatementMapper.SelectSpec> specBuilder) {
            this.specBuilder = specBuilder;
            return this;
        }

        public R2dbcSelectExecutorBuilder<T, R> criteriaHandler(BiFunction<R2dbcOperationParameter<T, R>, StatementMapper.SelectSpec, Optional<CriteriaDefinition>> criteriaHandler) {
            this.criteriaHandler = criteriaHandler;
            return this;
        }

        public R2dbcSelectExecutorBuilder<T, R> preparedOperationBuilder(BiFunction<R2dbcOperationParameter<T, R>, StatementMapper.SelectSpec, PreparedOperation<?>> preparedOperationBuilder) {
            this.preparedOperationBuilder = preparedOperationBuilder;
            return this;
        }

        public R2dbcSelectExecutorBuilder<T, R> rowMapperBuilder(Function<R2dbcOperationParameter<T, R>, BiFunction<Row, RowMetadata, R>> rowMapperBuilder) {
            this.rowMapperBuilder = rowMapperBuilder;
            return this;
        }

        public R2dbcSelectExecutor<T, R> buildExecutor() {
            return new R2dbcSelectExecutor<>(operationParameter, queryHandler, specBuilder, criteriaHandler, preparedOperationBuilder, rowMapperBuilder);
        }

        @Override
        protected R2dbcSelectExecutorBuilder<T, R> self() {
            return this;
        }

    }
}