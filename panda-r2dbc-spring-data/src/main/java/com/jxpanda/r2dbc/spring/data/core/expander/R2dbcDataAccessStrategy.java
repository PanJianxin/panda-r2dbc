/*
 * Copyright 2018-2022 the original author or authors.
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
package com.jxpanda.r2dbc.spring.data.core.expander;

import com.jxpanda.r2dbc.spring.data.convert.EntityRowMapper;
import com.jxpanda.r2dbc.spring.data.convert.MappingR2dbcConverter;
import com.jxpanda.r2dbc.spring.data.convert.R2dbcConverter;
import com.jxpanda.r2dbc.spring.data.convert.R2dbcCustomConversions;
import com.jxpanda.r2dbc.spring.data.core.DefaultStatementMapper;
import com.jxpanda.r2dbc.spring.data.core.StatementMapper;
import com.jxpanda.r2dbc.spring.data.dialect.R2dbcDialect;
import com.jxpanda.r2dbc.spring.data.mapping.OutboundRow;
import com.jxpanda.r2dbc.spring.data.mapping.R2dbcMappingContext;
import com.jxpanda.r2dbc.spring.data.query.UpdateMapper;
import com.jxpanda.r2dbc.spring.data.support.ArrayUtils;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.relational.core.dialect.ArrayColumns;
import org.springframework.data.relational.core.dialect.RenderContextFactory;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.BiFunction;

/**
 * 原 {@link org.springframework.r2dbc.core.DefaultReactiveDataAccessStrategy}，这个类实现了接口 {@link org.springframework.r2dbc.core.ReactiveDataAccessStrategy} 这个接口被标记为过期了
 * 而且对应的类已经从r2dbc的包里删除了，只是还存在于data-r2dbc的包里
 * 官方推荐直接使用{@link StatementMapper} , {@link UpdateMapper} , {@link R2dbcConverter}.来实现逻辑
 * 但是在编码过程中发现，这些类还是需要有一个对象来管理一下的，不然参数传递传一大堆，不优雅
 * 所以COPY过来改个名字，把接口的实现去掉，然后修改一下细节来使用
 *
 *
 * @author Mark Paluch
 * @author Louis Morgan
 * @author Jens Schauder
 * @author Panda
 */
@SuppressWarnings("JavadocReference")
public class R2dbcDataAccessStrategy {

    private final R2dbcDialect dialect;
    private final R2dbcConverter converter;
    private final UpdateMapper updateMapper;
    private final MappingContext<RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> mappingContext;
    private final StatementMapper statementMapper;
    private final NamedParameterExpander expander;


    /**
     * Creates a new {@link R2dbcDataAccessStrategy} given {@link R2dbcDialect} and optional
     * {@link org.springframework.core.convert.converter.Converter}s.
     *
     * @param dialect the {@link R2dbcDialect} to use.
     */
    public R2dbcDataAccessStrategy(R2dbcDialect dialect) {
        this(dialect, Collections.emptyList());
    }

    /**
     * Creates a new {@link R2dbcDataAccessStrategy} given {@link R2dbcDialect} and optional
     * {@link org.springframework.core.convert.converter.Converter}s.
     *
     * @param dialect    the {@link R2dbcDialect} to use.
     * @param converters custom converters to register, must not be {@literal null}.
     * @see R2dbcCustomConversions
     * @see org.springframework.core.convert.converter.Converter
     */
    public R2dbcDataAccessStrategy(R2dbcDialect dialect, Collection<?> converters) {
        this(dialect, createConverter(dialect, converters));
    }

    /**
     * Creates a new {@link R2dbcConverter} given {@link R2dbcDialect} and custom {@code converters}.
     *
     * @param dialect    must not be {@literal null}.
     * @param converters must not be {@literal null}.
     * @return the {@link R2dbcConverter}.
     */
    public static R2dbcConverter createConverter(R2dbcDialect dialect, Collection<?> converters) {

        Assert.notNull(dialect, "Dialect must not be null");
        Assert.notNull(converters, "Converters must not be null");

        R2dbcCustomConversions customConversions = R2dbcCustomConversions.of(dialect, converters);

        R2dbcMappingContext context = new R2dbcMappingContext();
        context.setSimpleTypeHolder(customConversions.getSimpleTypeHolder());

        return new MappingR2dbcConverter(context, customConversions);
    }

    /**
     * Creates a new {@link R2dbcDataAccessStrategy} given {@link R2dbcDialect} and {@link R2dbcConverter}.
     *
     * @param dialect   the {@link R2dbcDialect} to use.
     * @param converter must not be {@literal null}.
     */
    @SuppressWarnings("unchecked")
    public R2dbcDataAccessStrategy(R2dbcDialect dialect, R2dbcConverter converter) {

        Assert.notNull(dialect, "Dialect must not be null");
        Assert.notNull(converter, "RelationalConverter must not be null");

        this.converter = converter;
        this.updateMapper = new UpdateMapper(dialect, converter);
        this.mappingContext = (MappingContext<RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty>) this.converter
                .getMappingContext();
        this.dialect = dialect;

        RenderContextFactory factory = new RenderContextFactory(dialect);
        this.statementMapper = new DefaultStatementMapper(dialect, factory.createRenderContext(), this.updateMapper,
                this.mappingContext);

        expander = new NamedParameterExpander();

    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.function.ReactiveDataAccessStrategy#getAllColumns(java.lang.Class)
     */
    public List<SqlIdentifier> getAllColumns(Class<?> entityType) {

        RelationalPersistentEntity<?> persistentEntity = getPersistentEntity(entityType);

        if (persistentEntity == null) {
            return Collections.singletonList(SqlIdentifier.unquoted("*"));
        }

        List<SqlIdentifier> columnNames = new ArrayList<>();
        for (RelationalPersistentProperty property : persistentEntity) {
            columnNames.add(property.getColumnName());
        }

        return columnNames;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.function.ReactiveDataAccessStrategy#getIdentifierColumns(java.lang.Class)
     */
    public List<SqlIdentifier> getIdentifierColumns(Class<?> entityType) {

        RelationalPersistentEntity<?> persistentEntity = getRequiredPersistentEntity(entityType);

        List<SqlIdentifier> columnNames = new ArrayList<>();
        for (RelationalPersistentProperty property : persistentEntity) {

            if (property.isIdProperty()) {
                columnNames.add(property.getColumnName());
            }
        }

        return columnNames;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.function.ReactiveDataAccessStrategy#getOutboundRow(java.lang.Object)
     */
    public OutboundRow getOutboundRow(Object object) {

        Assert.notNull(object, "Entity object must not be null!");

        OutboundRow row = new OutboundRow();

        this.converter.write(object, row);

        RelationalPersistentEntity<?> entity = getRequiredPersistentEntity(ClassUtils.getUserClass(object));

        for (RelationalPersistentProperty property : entity) {

            Parameter value = row.get(property.getColumnName());
            if (shouldConvertArrayValue(property, value)) {

                Parameter writeValue = getArrayValue(value, property);
                row.put(property.getColumnName(), writeValue);
            }
        }

        return row;
    }

    private boolean shouldConvertArrayValue(RelationalPersistentProperty property, Parameter value) {

        if (!property.isCollectionLike()) {
            return false;
        }

        if (value.hasValue() && (value.getValue() instanceof Collection || Objects.requireNonNull(value.getValue()).getClass().isArray())) {
            return true;
        }

        return Collection.class.isAssignableFrom(value.getType()) || value.getType().isArray();
    }

    private Parameter getArrayValue(Parameter value, RelationalPersistentProperty property) {

        if (value.getType().equals(byte[].class)) {
            return value;
        }

        ArrayColumns arrayColumns = this.dialect.getArraySupport();

        if (!arrayColumns.isSupported()) {

            throw new InvalidDataAccessResourceUsageException(
                    "Dialect " + this.dialect.getClass().getName() + " does not support array columns");
        }

        Class<?> actualType = null;
        if (value.getValue() instanceof Collection) {
            actualType = CollectionUtils.findCommonElementType((Collection<?>) value.getValue());
        } else if (!value.isEmpty() && value.getValue().getClass().isArray()) {
            actualType = value.getValue().getClass().getComponentType();
        }

        if (actualType == null) {
            actualType = property.getActualType();
        }

        actualType = converter.getTargetType(actualType);

        if (value.isEmpty()) {

            Class<?> targetType = arrayColumns.getArrayType(actualType);
            int depth = actualType.isArray() ? ArrayUtils.getDimensionDepth(actualType) : 1;
            Class<?> targetArrayType = ArrayUtils.getArrayClass(targetType, depth);
            return Parameter.empty(targetArrayType);
        }

        assert value.getValue() != null;
        return Parameter.fromOrEmpty(this.converter.getArrayValue(arrayColumns, property, value.getValue()),
                actualType);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.function.ReactiveDataAccessStrategy#getBindValue(Parameter)
     */
    public Parameter getBindValue(Parameter value) {
        return this.updateMapper.getBindValue(value);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.function.ReactiveDataAccessStrategy#getRowMapper(java.lang.Class)
     */
    public <T> BiFunction<Row, RowMetadata, T> getRowMapper(Class<T> typeToRead) {
        return new EntityRowMapper<>(typeToRead, this.converter);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy#processNamedParameters(java.lang.String, org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy.NamedParameterProvider)
     */
    public PreparedOperation<?> processNamedParameters(String query, NamedParameterProvider parameterProvider) {

        List<String> parameterNames = this.expander.getParameterNames(query);

        Map<String, Parameter> namedBindings = new LinkedHashMap<>(parameterNames.size());
        for (String parameterName : parameterNames) {

            Parameter value = parameterProvider.getParameter(parameterNames.indexOf(parameterName), parameterName);

            if (value == null) {
                throw new InvalidDataAccessApiUsageException(
                        String.format("No parameter specified for [%s] in query [%s]", parameterName, query));
            }

            namedBindings.put(parameterName, value);
        }

        return this.expander.expand(query, this.dialect.getBindMarkersFactory(), new MapBindParameterSource(namedBindings));
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.function.ReactiveDataAccessStrategy#getTableName(java.lang.Class)
     */
    public SqlIdentifier getTableName(Class<?> type) {
        return getRequiredPersistentEntity(type).getTableName();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.function.ReactiveDataAccessStrategy#toSql(SqlIdentifier)
     */
    public String toSql(SqlIdentifier identifier) {
        return this.updateMapper.toSql(identifier);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.function.ReactiveDataAccessStrategy#getStatementMapper()
     */
    public StatementMapper getStatementMapper() {
        return this.statementMapper;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.function.ReactiveDataAccessStrategy#getConverter()
     */
    public R2dbcConverter getConverter() {
        return this.converter;
    }

    public MappingContext<RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> getMappingContext() {
        return this.mappingContext;
    }

    public String renderForGeneratedValues(SqlIdentifier identifier) {
        return dialect.renderForGeneratedValues(identifier);
    }

    private RelationalPersistentEntity<?> getRequiredPersistentEntity(Class<?> typeToRead) {
        return this.mappingContext.getRequiredPersistentEntity(typeToRead);
    }

    @Nullable
    private RelationalPersistentEntity<?> getPersistentEntity(Class<?> typeToRead) {
        return this.mappingContext.getPersistentEntity(typeToRead);
    }
}
