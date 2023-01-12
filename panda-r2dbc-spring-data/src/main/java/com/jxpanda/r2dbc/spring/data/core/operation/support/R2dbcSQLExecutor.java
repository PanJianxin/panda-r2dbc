package com.jxpanda.r2dbc.spring.data.core.operation.support;

import com.jxpanda.r2dbc.spring.data.config.R2dbcMappingProperties;
import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.core.query.LambdaCriteria;
import com.jxpanda.r2dbc.spring.data.extension.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.extension.annotation.TableEntity;
import com.jxpanda.r2dbc.spring.data.extension.annotation.TableLogic;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.Getter;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.ProjectionInformation;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.r2dbc.convert.EntityRowMapper;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.data.relational.core.sql.*;
import org.springframework.data.util.Pair;
import org.springframework.data.util.ProxyUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.r2dbc.core.RowsFetchSpec;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.beans.FeatureDescriptor;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Getter
@SuppressWarnings({"unchecked", "rawtypes", "deprecation", "unused", "SameParameterValue"})
public final class R2dbcSQLExecutor {

    private static final String SQL_AS = " AS ";

    private final ReactiveEntityTemplate template;

    private final R2dbcMappingProperties r2dbcMappingProperties;

    private final SpelAwareProxyProjectionFactory projectionFactory;

    private final MappingContext<? extends RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> mappingContext;

    private final StatementMapper statementMapper;

    private final R2dbcConverter converter;

    private final DatabaseClient databaseClient;

    public R2dbcSQLExecutor(ReactiveEntityTemplate template, R2dbcDialect dialect, R2dbcConverter converter) {
        this.template = template;
        this.databaseClient = template.getDatabaseClient();
        this.converter = converter;
        this.mappingContext = this.converter.getMappingContext();
        this.projectionFactory = template.getProjectionFactory();
        this.r2dbcMappingProperties = template.getR2dbcMappingProperties();
        this.statementMapper = template.getDataAccessStrategy().getStatementMapper();
    }


    <T> Mono<Boolean> doExists(Query query, Class<T> entityClass, SqlIdentifier tableName) {
        return doExists(query, entityClass, tableName, false);
    }

    <T> Mono<Boolean> doExists(Query query, Class<T> entityClass, SqlIdentifier tableName, boolean ignoreLogicDelete) {

        RelationalPersistentEntity<T> entity = getRequiredEntity(entityClass);
        StatementMapper statementMapper = getStatementMapper().forType(entityClass);

        SqlIdentifier columnName = entity.hasIdProperty() ? entity.getRequiredIdProperty().getColumnName() : SqlIdentifier.unquoted("*");

        StatementMapper.SelectSpec selectSpec = statementMapper.createSelect(tableName).withProjection(columnName).limit(1);

        selectSpec = selectWithCriteria(selectSpec, query, entityClass, ignoreLogicDelete);

        PreparedOperation<?> operation = statementMapper.getMappedObject(selectSpec);

        return getDatabaseClient().sql(operation).map((r, md) -> r).first().hasElement();
    }

    <T> Mono<Long> doCount(Query query, Class<T> entityClass, SqlIdentifier tableName) {
        return doCount(query, entityClass, tableName, false);
    }

    <T> Mono<Long> doCount(Query query, Class<T> entityClass, SqlIdentifier tableName, boolean ignoreLogicDelete) {

        RelationalPersistentEntity<T> entity = getRequiredEntity(entityClass);
        StatementMapper statementMapper = getStatementMapper().forType(entityClass);

        StatementMapper.SelectSpec selectSpec = statementMapper.createSelect(tableName).doWithTable((table, spec) -> {
            Expression countExpression = entity.hasIdProperty() ? table.column(entity.getRequiredIdProperty().getColumnName()) : Expressions.asterisk();
            return spec.withProjection(Functions.count(countExpression));
        });

        selectSpec = selectWithCriteria(selectSpec, query, entityClass, ignoreLogicDelete);

        PreparedOperation<?> operation = statementMapper.getMappedObject(selectSpec);

        return getDatabaseClient().sql(operation).map((r, md) -> r.get(0, Long.class)).first().defaultIfEmpty(0L);
    }

    <T, R> RowsFetchSpec<R> doSelect(Query query, Class<T> entityClass, SqlIdentifier tableName, Class<R> returnType) {
        return doSelect(query, entityClass, tableName, returnType, false);
    }

    <T, R> RowsFetchSpec<R> doSelect(Query query, Class<T> entityClass, SqlIdentifier tableName, Class<R> returnType, boolean ignoreLogicDelete) {

        boolean isQueryEntity = false;

        if (entityClass.isAnnotationPresent(TableEntity.class)) {
            isQueryEntity = entityClass.getAnnotation(TableEntity.class).isAggregate();
        }

        StatementMapper statementMapper = isQueryEntity ? getStatementMapper() : getStatementMapper().forType(entityClass);

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

        return getRowsFetchSpec(getDatabaseClient().sql(operation), entityClass, returnType);
    }


    <T> Mono<T> doInsert(T entity, SqlIdentifier tableName) {

        RelationalPersistentEntity<T> persistentEntity = getRequiredEntity(entity);

        return template.maybeCallBeforeConvert(entity, tableName).flatMap(onBeforeConvert -> {

            T initializedEntity = setVersionIfNecessary(persistentEntity, onBeforeConvert);

            OutboundRow outboundRow = template.getDataAccessStrategy().getOutboundRow(initializedEntity);

            potentiallyRemoveId(persistentEntity, outboundRow);

            return template.maybeCallBeforeSave(initializedEntity, outboundRow, tableName).flatMap(entityToSave -> doInsert(entityToSave, tableName, outboundRow));
        });
    }

    <T> Mono<T> doInsert(T entity, SqlIdentifier tableName, OutboundRow outboundRow) {

        StatementMapper mapper = getStatementMapper();
        StatementMapper.InsertSpec insert = mapper.createInsert(tableName);

        for (SqlIdentifier column : outboundRow.keySet()) {
            Parameter settableValue = outboundRow.get(column);
            if (settableValue.hasValue()) {
                insert = insert.withColumn(column, settableValue);
            }
        }

        PreparedOperation<?> operation = mapper.getMappedObject(insert);


        List<SqlIdentifier> identifierColumns = getIdentifierColumns(entity.getClass());

        return getDatabaseClient().sql(operation).filter(statement -> {

            if (identifierColumns.isEmpty()) {
                return statement.returnGeneratedValues();
            }

            return statement.returnGeneratedValues(template.getDataAccessStrategy().renderForGeneratedValues(identifierColumns.get(0)));
        }).map(getConverter().populateIdIfNecessary(entity)).all().last(entity).flatMap(saved -> template.maybeCallAfterSave(saved, outboundRow, tableName));
    }

    <T> Mono<Long> doDelete(T entity, SqlIdentifier tableName) {
        return doDelete(entity, tableName, false);
    }

    <T> Mono<Long> doDelete(T entity, SqlIdentifier tableName, boolean ignoreLogicDelete) {
        RelationalPersistentEntity<T> persistentEntity = getRequiredEntity(entity);
        return doDelete(getByIdQuery(entity, persistentEntity), persistentEntity.getType(), tableName, ignoreLogicDelete);
    }

    Mono<Long> doDelete(Query query, Class<?> entityClass, SqlIdentifier tableName) {
        return doDelete(query, entityClass, tableName, false);
    }

    Mono<Long> doDelete(Query query, Class<?> entityClass, SqlIdentifier tableName, boolean ignoreLogicDelete) {

        // 如果开启了逻辑删除，变为执行更新操作
        if (isLogicDeleteEnable(entityClass, ignoreLogicDelete)) {
            return doUpdate(query, createLogicDeleteUpdate(entityClass), entityClass, tableName);
        }

        StatementMapper statementMapper = getStatementMapper().forType(entityClass);

        StatementMapper.DeleteSpec deleteSpec = statementMapper.createDelete(tableName);

        Optional<CriteriaDefinition> criteria = query.getCriteria();
        if (criteria.isPresent()) {
            deleteSpec = criteria.map(deleteSpec::withCriteria).orElse(deleteSpec);
        }

        PreparedOperation<?> operation = statementMapper.getMappedObject(deleteSpec);
        return getDatabaseClient().sql(operation).fetch().rowsUpdated().defaultIfEmpty(0L);
    }


    <T> Mono<T> doUpdate(T entity, SqlIdentifier tableName) {


        RelationalPersistentEntity<T> persistentEntity = getRequiredEntity(entity);

        return template.maybeCallBeforeConvert(entity, tableName).flatMap(onBeforeConvert -> {

            T entityToUse;
            Criteria matchingVersionCriteria;

            if (persistentEntity.hasVersionProperty()) {
                matchingVersionCriteria = createMatchingVersionCriteria(onBeforeConvert, persistentEntity);
                entityToUse = incrementVersion(persistentEntity, onBeforeConvert);
            } else {
                entityToUse = onBeforeConvert;
                matchingVersionCriteria = null;
            }

            OutboundRow outboundRow = template.getDataAccessStrategy().getOutboundRow(entityToUse);

            return template.maybeCallBeforeSave(entityToUse, outboundRow, tableName).flatMap(onBeforeSave -> {

                SqlIdentifier idColumn = persistentEntity.getRequiredIdProperty().getColumnName();
                Parameter id = outboundRow.remove(idColumn);

                persistentEntity.forEach(p -> {
                    if (p.isInsertOnly()) {
                        outboundRow.remove(p.getColumnName());
                    }
                });

                Criteria criteria = Criteria.where(template.getDataAccessStrategy().toSql(idColumn)).is(id);

                if (matchingVersionCriteria != null) {
                    criteria = criteria.and(matchingVersionCriteria);
                }

                return doUpdate(onBeforeSave, tableName, persistentEntity, criteria, outboundRow);
            });
        });
    }

    Mono<Long> doUpdate(Query query, Update update, Class<?> entityClass, SqlIdentifier tableName) {

        StatementMapper statementMapper = getStatementMapper().forType(entityClass);

        StatementMapper.UpdateSpec selectSpec = statementMapper.createUpdate(tableName, update);

        Optional<CriteriaDefinition> criteria = query.getCriteria();
        if (criteria.isPresent()) {
            selectSpec = criteria.map(selectSpec::withCriteria).orElse(selectSpec);
        }

        PreparedOperation<?> operation = statementMapper.getMappedObject(selectSpec);
        return getDatabaseClient().sql(operation).fetch().rowsUpdated();
    }

    <T> Mono<T> doUpdate(T entity, SqlIdentifier tableName, RelationalPersistentEntity<T> persistentEntity, Criteria criteria, OutboundRow outboundRow) {


        Update update = Update.from((Map) outboundRow);

        StatementMapper mapper = getStatementMapper();
        StatementMapper.UpdateSpec updateSpec = mapper.createUpdate(tableName, update).withCriteria(criteria);

        PreparedOperation<?> operation = mapper.getMappedObject(updateSpec);

        return getDatabaseClient().sql(operation).fetch().rowsUpdated().handle((rowsUpdated, sink) -> {

            if (rowsUpdated != 0) {
                return;
            }

            if (persistentEntity.hasVersionProperty()) {
                sink.error(new OptimisticLockingFailureException(formatOptimisticLockingExceptionMessage(entity, persistentEntity)));
            } else {
                sink.error(new TransientDataAccessResourceException(formatTransientEntityExceptionMessage(entity, persistentEntity)));
            }
        }).then(template.maybeCallAfterSave(entity, outboundRow, tableName));
    }

    private List<SqlIdentifier> getIdentifierColumns(Class<?> clazz) {
        return template.getDataAccessStrategy().getIdentifierColumns(clazz);
    }

    public <T> SqlIdentifier getTableName(Class<T> entityClass) {
        return getRequiredEntity(entityClass).getTableName();
    }

    public <T> SqlIdentifier getTableNameOrEmpty(Class<T> entityClass) {

        RelationalPersistentEntity<T> entity = getPersistentEntity(entityClass);

        return entity != null ? entity.getTableName() : SqlIdentifier.EMPTY;
    }


    @Nullable
    public <T> RelationalPersistentEntity<T> getPersistentEntity(Class<T> entityClass) {
        return (RelationalPersistentEntity<T>) getMappingContext().getPersistentEntity(entityClass);
    }

    public <T> RelationalPersistentEntity<T> getRequiredEntity(T entity) {
        Class<?> entityType = ProxyUtils.getUserClass(entity);
        return (RelationalPersistentEntity) getRequiredEntity(entityType);
    }

    public <T> RelationalPersistentEntity<T> getRequiredEntity(Class<T> entityClass) {
        return (RelationalPersistentEntity<T>) getMappingContext().getRequiredPersistentEntity(entityClass);
    }


    private <T> boolean isAggregateEntity(Class<T> entityClass) {
        RelationalPersistentEntity<T> persistentEntity = getPersistentEntity(entityClass);
        return persistentEntity != null && isAggregateEntity(persistentEntity);
    }

    private <T> boolean isAggregateEntity(RelationalPersistentEntity<T> relationalPersistentEntity) {
        TableEntity tableEntity = relationalPersistentEntity.findAnnotation(TableEntity.class);
        return tableEntity != null && tableEntity.isAggregate();
    }

    private boolean isPropertyExists(RelationalPersistentProperty property) {
        TableColumn tableColumn = property.findAnnotation(TableColumn.class);
        return property.isIdProperty() || (tableColumn != null && tableColumn.exists());
    }

    private boolean isFunctionProperty(RelationalPersistentProperty property) {
        TableColumn tableColumn = property.findAnnotation(TableColumn.class);
        return tableColumn != null && !ObjectUtils.isEmpty(tableColumn.function());
    }

    private <T> Query getByIdQuery(T entity, RelationalPersistentEntity<?> persistentEntity) {

        if (!persistentEntity.hasIdProperty()) {
            throw new MappingException("No id property found for object of type " + persistentEntity.getType());
        }

        IdentifierAccessor identifierAccessor = persistentEntity.getIdentifierAccessor(entity);
        Object id = identifierAccessor.getRequiredIdentifier();

        return Query.query(Criteria.where(persistentEntity.getRequiredIdProperty().getName()).is(id));
    }

    private <T> BiFunction<Row, RowMetadata, T> getRowMapper(Class<T> typeToRead) {
        return new EntityRowMapper<>(typeToRead, getConverter());
    }

    private <T, R> List<Expression> getSelectProjection(Table table, Query query, Class<T> entityClass, Class<R> returnType) {

        if (query.getColumns().isEmpty()) {

            if (returnType.isInterface()) {

                ProjectionInformation projectionInformation = getProjectionFactory().getProjectionInformation(returnType);

                if (projectionInformation.isClosed()) {
                    return projectionInformation.getInputProperties().stream().map(FeatureDescriptor::getName).map(table::column).collect(Collectors.toList());
                }
            }

            RelationalPersistentEntity<T> entity = getRequiredEntity(entityClass);

            List<Expression> columns = new ArrayList<>();

            boolean isQueryEntity = isAggregateEntity(entityClass);

            entity.forEach(property -> {
                if (isPropertyExists(property)) {
                    Expression expression;
                    if (!isQueryEntity) {
                        expression = table.column(property.getColumnName());
                    } else {
                        TableColumn tableColumn = property.getRequiredAnnotation(TableColumn.class);
                        if (!isFunctionProperty(property)) {
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


    private <T, R> RowsFetchSpec<R> getRowsFetchSpec(DatabaseClient.GenericExecuteSpec executeSpec, Class<T> entityClass, Class<R> returnType) {

        boolean simpleType;

        BiFunction<Row, RowMetadata, R> rowMapper;
        if (returnType.isInterface()) {
            simpleType = getConverter().isSimpleType(entityClass);
            rowMapper = getRowMapper(entityClass).andThen(source -> getProjectionFactory().createProjection(returnType, source));
        } else {
            simpleType = getConverter().isSimpleType(returnType);
            rowMapper = getRowMapper(returnType);
        }

        // avoid top-level null values if the read type is a simple one (e.g. SELECT MAX(age) via Integer.class)
        if (simpleType) {
            return new UnwrapOptionalFetchSpecAdapter<>(executeSpec.map((row, metadata) -> Optional.ofNullable(rowMapper.apply(row, metadata))));
        }

        return executeSpec.map(rowMapper);
    }

    private <T> T setVersionIfNecessary(RelationalPersistentEntity<T> persistentEntity, T entity) {

        RelationalPersistentProperty versionProperty = persistentEntity.getVersionProperty();
        if (versionProperty == null) {
            return entity;
        }

        Class<?> versionPropertyType = versionProperty.getType();
        Long version = versionPropertyType.isPrimitive() ? 1L : 0L;
        ConversionService conversionService = getConverter().getConversionService();
        PersistentPropertyAccessor<?> propertyAccessor = persistentEntity.getPropertyAccessor(entity);
        propertyAccessor.setProperty(versionProperty, conversionService.convert(version, versionPropertyType));

        return (T) propertyAccessor.getBean();
    }

    private void potentiallyRemoveId(RelationalPersistentEntity<?> persistentEntity, OutboundRow outboundRow) {

        RelationalPersistentProperty idProperty = persistentEntity.getIdProperty();
        if (idProperty == null) {
            return;
        }

        SqlIdentifier columnName = idProperty.getColumnName();
        Parameter parameter = outboundRow.get(columnName);

        if (shouldSkipIdValue(parameter, idProperty)) {
            outboundRow.remove(columnName);
        }
    }

    private boolean shouldSkipIdValue(@Nullable Parameter value, RelationalPersistentProperty property) {

        if (value == null || value.getValue() == null) {
            return true;
        }

        if (value.getValue() instanceof Number numberValue) {
            return numberValue.longValue() == 0L;
        }

        return false;
    }

    private <T> Criteria createMatchingVersionCriteria(T entity, RelationalPersistentEntity<T> persistentEntity) {

        PersistentPropertyAccessor<?> propertyAccessor = persistentEntity.getPropertyAccessor(entity);

        Optional<RelationalPersistentProperty> versionPropertyOptional = Optional.ofNullable(persistentEntity.getVersionProperty());

        return versionPropertyOptional.map(versionProperty -> {
            Object version = propertyAccessor.getProperty(versionProperty);
            Criteria.CriteriaStep versionColumn = Criteria.where(template.getDataAccessStrategy().toSql(versionProperty.getColumnName()));
            if (version == null) {
                return versionColumn.isNull();
            } else {
                return versionColumn.is(version);
            }
        }).orElse(Criteria.empty());

    }

    private <T> T incrementVersion(RelationalPersistentEntity<T> persistentEntity, T entity) {

        PersistentPropertyAccessor<?> propertyAccessor = persistentEntity.getPropertyAccessor(entity);

        Optional<RelationalPersistentProperty> versionPropertyOptional = Optional.ofNullable(persistentEntity.getVersionProperty());

        versionPropertyOptional.ifPresent(versionProperty -> {
            ConversionService conversionService = getConverter().getConversionService();
            Optional<Object> currentVersionValue = Optional.ofNullable(propertyAccessor.getProperty(versionProperty));

            long newVersionValue = currentVersionValue.map(it -> conversionService.convert(it, Long.class)).map(it -> it + 1).orElse(1L);

            propertyAccessor.setProperty(versionProperty, conversionService.convert(newVersionValue, versionProperty.getType()));
        });
        return (T) propertyAccessor.getBean();
    }

    /**
     * 获取对象中逻辑删除的字段和值
     *
     * @param entityClass entityClass
     * @return 第一个值是字段名，第二个值是逻辑删除的删除值
     */
    private Pair<String, Object> getLogicDeleteColumn(Class<?> entityClass, LogicDeleteValue whichValue) {
        RelationalPersistentEntity<?> requiredEntity = getRequiredEntity(entityClass);
        RelationalPersistentProperty logicDeleteProperty = requiredEntity.getPersistentProperty(TableLogic.class);
        // 默认取值是全局配置的逻辑删除字段
        R2dbcMappingProperties.LogicDelete logicDeleteConfig = r2dbcMappingProperties.logicDelete();
        String logicDeleteField = logicDeleteConfig.field();
        Object value = switch (whichValue) {
            case DELETE_VALUE -> logicDeleteConfig.deleteValue();
            case UNDELETE_VALUE -> logicDeleteConfig.undeleteValue();
        };
        // 如果配置了注解，则以注解为准
        if (logicDeleteProperty != null) {
            logicDeleteField = logicDeleteProperty.getName();
            TableLogic tableLogicAnnotation = logicDeleteProperty.getRequiredAnnotation(TableLogic.class);
            value = switch (whichValue) {
                case DELETE_VALUE -> tableLogicAnnotation.deleteValue().getSupplier().get();
                case UNDELETE_VALUE -> tableLogicAnnotation.undeleteValue().getSupplier().get();
            };
        }
        return Pair.of(logicDeleteField, value);
    }


    /**
     * 创建逻辑删除的Update对象
     *
     * @param entityClass entityClass
     */
    private Update createLogicDeleteUpdate(Class<?> entityClass) {
        Pair<String, Object> logicDeleteColumn = getLogicDeleteColumn(entityClass, LogicDeleteValue.DELETE_VALUE);
        return Update.update(logicDeleteColumn.getFirst(), logicDeleteColumn.getSecond());
    }

    private <T> StatementMapper.SelectSpec selectWithCriteria(StatementMapper.SelectSpec selectSpec, Query query, Class<T> entityClass, boolean ignoreLogicDelete) {
        Optional<CriteriaDefinition> criteriaOptional = query.getCriteria();
        if (isLogicDeleteEnable(entityClass, ignoreLogicDelete)) {
            criteriaOptional = query.getCriteria()
                    .or(() -> Optional.of(Criteria.empty()))
                    .map(criteriaDefinition -> {
                        // 获取查询对象中的逻辑删除字段和值，写入到criteria中
                        Pair<String, Object> logicDeleteColumn = getLogicDeleteColumn(entityClass, LogicDeleteValue.UNDELETE_VALUE);
                        if (criteriaDefinition instanceof Criteria criteria) {
                            return criteria.and(Criteria.where(logicDeleteColumn.getFirst()).is(logicDeleteColumn.getSecond()));
                        }
                        if (criteriaDefinition instanceof LambdaCriteria lambdaCriteria) {
                            return lambdaCriteria.and(LambdaCriteria.where(logicDeleteColumn.getFirst()).is(logicDeleteColumn.getSecond()));
                        }
                        return criteriaDefinition;
                    });
        }
        return criteriaOptional.map(selectSpec::withCriteria).orElse(selectSpec);
    }

    /**
     * 是否使用逻辑删除
     * 判定依据是：
     * 1、类注解优先，如果类配置了${@link TableLogic}注解的enable属性为false，则不执行逻辑删除
     * 2、全局配置，如果没有配置注解，看全局是否配置逻辑删除，以全局的配置为主
     *
     * @param entityClass       entityClass
     * @param ignoreLogicDelete 是否忽略逻辑删除，这个的优先级高于所有配置
     */
    private boolean isLogicDeleteEnable(Class<?> entityClass, boolean ignoreLogicDelete) {

        if (ignoreLogicDelete) {
            return false;
        }

        RelationalPersistentEntity<?> requiredEntity = getRequiredEntity(entityClass);
        RelationalPersistentProperty logicDeleteProperty = requiredEntity.getPersistentProperty(TableLogic.class);
        if (logicDeleteProperty == null) {
            // 如果没有配置逻辑删除的字段，以全局配置为准
            R2dbcMappingProperties.LogicDelete logicDeleteConfig = r2dbcMappingProperties.logicDelete();
            // 开启了逻辑删除配置，并且配置了逻辑删除字段才生效
            return logicDeleteConfig.enable() && !ObjectUtils.isEmpty(logicDeleteConfig.field());
        } else {
            // 如果配置了逻辑删除字段，以注解的配置为准
            TableLogic tableLogicAnnotation = logicDeleteProperty.getRequiredAnnotation(TableLogic.class);
            return tableLogicAnnotation.enable();
        }
    }

    private <T> String formatOptimisticLockingExceptionMessage(T entity, RelationalPersistentEntity<T> persistentEntity) {

        return String.format("Failed to update table [%s]; Version does not match for row with Id [%s]", persistentEntity.getQualifiedTableName(), persistentEntity.getIdentifierAccessor(entity).getIdentifier());
    }

    private <T> String formatTransientEntityExceptionMessage(T entity, RelationalPersistentEntity<T> persistentEntity) {

        return String.format("Failed to update table [%s]; Row with Id [%s] does not exist", persistentEntity.getQualifiedTableName(), persistentEntity.getIdentifierAccessor(entity).getIdentifier());
    }

    private enum LogicDeleteValue {
        UNDELETE_VALUE,
        DELETE_VALUE
    }

    private record UnwrapOptionalFetchSpecAdapter<T>(RowsFetchSpec<Optional<T>> delegate) implements RowsFetchSpec<T> {

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
