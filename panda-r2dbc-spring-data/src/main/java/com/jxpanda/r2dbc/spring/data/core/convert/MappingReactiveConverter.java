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
package com.jxpanda.r2dbc.spring.data.core.convert;

import com.jxpanda.r2dbc.spring.data.config.R2dbcMappingProperties;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableEntity;
import com.jxpanda.r2dbc.spring.data.core.enhance.policy.ValidationPolicy;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mapping.*;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.*;
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.r2dbc.mapping.R2dbcMappingContext;
import org.springframework.data.relational.core.conversion.RelationalConverter;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * Converter for R2DBC.
 *
 * @author Mark Paluch
 * @author Oliver Drotbohm
 */
@SuppressWarnings("deprecation")
public class MappingReactiveConverter extends MappingR2dbcConverter {

    private final R2dbcCustomTypeHandlers typeHandlers;

    private final R2dbcMappingProperties mappingProperties;

    /**
     * Creates a new {@link MappingReactiveConverter} given {@link MappingContext} and {@link CustomConversions} and {@link R2dbcCustomTypeHandlers}.
     *
     * @param context      must not be {@literal null}.
     * @param conversions  must not be {@literal null}.
     * @param typeHandlers must not be {@literal null}.
     */
    public MappingReactiveConverter(
            MappingContext<? extends RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> context,
            CustomConversions conversions,
            R2dbcCustomTypeHandlers typeHandlers,
            R2dbcMappingProperties mappingProperties) {
        super(context, conversions);
        this.typeHandlers = typeHandlers;
        this.mappingProperties = mappingProperties;
    }

    public R2dbcCustomTypeHandlers getTypeHandlers() {
        return typeHandlers;
    }

    // ----------------------------------
    // Entity reading
    // ----------------------------------

    /*
     * (non-Javadoc)
     * @see org.springframework.data.convert.EntityReader#read(java.lang.Class, S)
     */
    @Override
    public <R> R read(Class<R> type, Row row) {
        return read(type, row, null);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.convert.R2dbcConverter#read(java.lang.Class, io.r2dbc.spi.Row, io.r2dbc.spi.RowMetadata)
     */
    @Override
    @SuppressWarnings("ConstantConditions")
    public <R> R read(Class<R> type, Row row, @Nullable RowMetadata metadata) {

//        TypeInformation<? extends R> typeInfo = ClassTypeInformation.from(type);
        TypeInformation<R> typeInfo = TypeInformation.of(type);
        Class<? extends R> rawType = typeInfo.getType();

        if (Row.class.isAssignableFrom(rawType)) {
            return type.cast(row);
        }

        if (getConversions().hasCustomReadTarget(Row.class, rawType)
                && getConversionService().canConvert(Row.class, rawType)) {
            return getConversionService().convert(row, rawType);
        }

        return read(getRequiredPersistentEntity(type), row, metadata);
    }

    private <R> R read(RelationalPersistentEntity<R> entity, Row row, @Nullable RowMetadata metadata) {

        R result = createInstance(row, metadata, "", entity);

        if (entity.requiresPropertyPopulation()) {
            ConvertingPropertyAccessor<R> propertyAccessor = new ConvertingPropertyAccessor<>(
                    entity.getPropertyAccessor(result), getConversionService());

            for (RelationalPersistentProperty property : entity) {

                if (entity.isCreatorArgument(property)) {
                    continue;
                }

                Object value = readFrom(row, metadata, property, "");

                if (value != null) {
                    propertyAccessor.setProperty(property, value);
                }
            }
        }

        return result;
    }

    /**
     * Read a single value or a complete Entity from the {@link Row} passed as an argument.
     *
     * @param row      the {@link Row} to extract the value from. Must not be {@literal null}.
     * @param metadata the {@link RowMetadata}. Can be {@literal null}.
     * @param property the {@link RelationalPersistentProperty} for which the value is intended. Must not be
     *                 {@literal null}.
     * @param prefix   to be used for all column names accessed by this method. Must not be {@literal null}.
     * @return the value read from the {@link Row}. May be {@literal null}.
     */
    @Nullable
    private Object readFrom(Row row, @Nullable RowMetadata metadata, RelationalPersistentProperty property,
                            String prefix) {

        String identifier = prefix + getPropertyName(property);

        try {

            Object value = null;
            if (metadata == null || RowMetadataUtils.containsColumn(metadata, identifier)) {
                value = row.get(identifier);
            }

            if (value == null) {
                return null;
            }

            // handler的优先级高于conversions
            // handler是通过TableColumn注解来指定的，因此优先级应该高于全局的conversions
            if (getTypeHandlers().hasTypeHandler(property)) {
                return getTypeHandlers().read(value, property);
            }

            if (getConversions().hasCustomReadTarget(value.getClass(), property.getType())) {
                return readValue(value, property.getTypeInformation());
            }

            if (property.isEntity()) {
                return readEntityFrom(row, metadata, property);
            }

            return readValue(value, property.getTypeInformation());

        } catch (Exception o_O) {
            throw new MappingException(String.format("Could not read property %s from column %s!", property, identifier),
                    o_O);
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private <S> S readEntityFrom(Row row, @Nullable RowMetadata metadata, PersistentProperty<?> property) {

        String prefix = property.getName() + "_";

        RelationalPersistentEntity<?> entity = getMappingContext().getRequiredPersistentEntity(property.getActualType());

        if (entity.hasIdProperty()) {
            if (readFrom(row, metadata, entity.getRequiredIdProperty(), prefix) == null) {
                return null;
            }
        }

        Object instance = createInstance(row, metadata, prefix, entity);

        if (entity.requiresPropertyPopulation()) {
            PersistentPropertyAccessor<?> accessor = entity.getPropertyAccessor(instance);
            ConvertingPropertyAccessor<?> propertyAccessor = new ConvertingPropertyAccessor<>(accessor,
                    getConversionService());

            for (RelationalPersistentProperty p : entity) {
                if (!entity.isCreatorArgument(property)) {
                    propertyAccessor.setProperty(p, readFrom(row, metadata, p, prefix));
                }
            }
        }

        return (S) instance;
    }

    private <S> S createInstance(Row row, @Nullable RowMetadata rowMetadata, String prefix,
                                 RelationalPersistentEntity<S> entity) {

        InstanceCreatorMetadata<RelationalPersistentProperty> persistenceConstructor = entity.getInstanceCreatorMetadata();
        ParameterValueProvider<RelationalPersistentProperty> provider;

        if (persistenceConstructor != null && persistenceConstructor.hasParameters()) {

            SpELContext spELContext = new SpELContext(new RowPropertyAccessor(rowMetadata));
            SpELExpressionEvaluator expressionEvaluator = new DefaultSpELExpressionEvaluator(row, spELContext);
            provider = new SpELExpressionParameterValueProvider<>(expressionEvaluator, getConversionService(),
                    new RowParameterValueProvider(row, rowMetadata, entity, this, prefix));
        } else {
            provider = NoOpParameterValueProvider.INSTANCE;
        }

        return createInstance(entity, provider::getParameterValue);
    }

    // ----------------------------------
    // Entity writing
    // ----------------------------------

    /*
     * (non-Javadoc)
     * @see org.springframework.data.convert.EntityWriter#write(java.lang.Object, java.lang.Object)
     */
    @Override
    public void write(Object source, OutboundRow outboundRow) {

        Class<?> userClass = ClassUtils.getUserClass(source);

        Optional<Class<?>> customTarget = getConversions().getCustomWriteTarget(userClass, OutboundRow.class);
        if (customTarget.isPresent()) {

            OutboundRow result = getConversionService().convert(source, OutboundRow.class);
            if (result != null) {
                outboundRow.putAll(result);
            }
            return;
        }

        writeInternal(source, outboundRow, userClass);
    }

    private void writeInternal(Object source, OutboundRow outboundRow, Class<?> userClass) {

        RelationalPersistentEntity<?> entity = getRequiredPersistentEntity(userClass);
        PersistentPropertyAccessor<?> propertyAccessor = entity.getPropertyAccessor(source);

        writeProperties(outboundRow, entity, propertyAccessor, entity.isNew(source));
    }

    private void writeProperties(OutboundRow outboundRow, RelationalPersistentEntity<?> entity,
                                 PersistentPropertyAccessor<?> accessor, boolean isNew) {

        for (RelationalPersistentProperty property : entity) {

            if (!property.isWritable() || !isPropertyExists(property)) {
                continue;
            }

            Object value;

            if (property.isIdProperty()) {
                IdentifierAccessor identifierAccessor = entity.getIdentifierAccessor(accessor.getBean());
                value = identifierAccessor.getIdentifier();
            } else if (typeHandlers.hasTypeHandler(property)) {
                value = typeHandlers.write(accessor.getProperty(property), property);
            } else {
                value = accessor.getProperty(property);
            }

            if (!isPropertyEffective(entity, property, value)) {
                // 基于配置进行一次过滤
                continue;
            } else if (value == null) {
                // 这个是原始逻辑，如果值为null，写入一个null，然后交由数据库驱动层来处理
                writeNullInternal(outboundRow, property);
                continue;
            }

            if (isSimpleType(value.getClass())) {
                writeSimpleInternal(outboundRow, value, isNew, property);
            } else {
                writePropertyInternal(outboundRow, value, isNew, property);
            }
        }
    }

    @SuppressWarnings("unused")
    private void writeSimpleInternal(OutboundRow outboundRow, Object value, boolean isNew,
                                     RelationalPersistentProperty property) {

        Object result = getPotentiallyConvertedSimpleWrite(value);

        outboundRow.put(property.getColumnName(),
                Parameter.fromOrEmpty(result, getPotentiallyConvertedSimpleNullType(property.getType())));
    }

    private void writePropertyInternal(OutboundRow outboundRow, Object value, boolean isNew,
                                       RelationalPersistentProperty property) {

//        TypeInformation<?> valueType = ClassTypeInformation.from(value.getClass());
        TypeInformation<?> valueType = TypeInformation.of(value.getClass());

        if (valueType.isCollectionLike()) {

            if (valueType.getActualType() != null && valueType.getRequiredActualType().isCollectionLike()) {

                // pass-thru nested collections
                writeSimpleInternal(outboundRow, value, isNew, property);
                return;
            }

            List<Object> collectionInternal = createCollection(asCollection(value), property);
            outboundRow.put(property.getColumnName(), Parameter.from(collectionInternal));
            return;
        }

        throw new InvalidDataAccessApiUsageException("Nested entities are not supported");
    }

    /**
     * 返回字段是否是存在的
     * 主要用于排除虚拟字段
     */
    private boolean isPropertyExists(RelationalPersistentProperty property) {
        TableColumn tableColumn = property.findAnnotation(TableColumn.class);
        return property.isIdProperty() || (tableColumn != null && tableColumn.exists());
    }

    /**
     * 返回字段的值是否有效
     * 处理空值，在插入/更新数据的时候判定是否需要过滤掉对应字段的判别依据
     */
    private boolean isPropertyEffective(RelationalPersistentEntity<?> entity, RelationalPersistentProperty property, @Nullable Object value) {

        // 判别优先级
        // 字段上的配置 > 类上的配置 > 全局配置文件的配置
        // TableId注解不需要判断，因为Id的语义本来就是强制存在的

        // 全局校验策略
        ValidationPolicy validationPolicy = mappingProperties.validationPolicy();
        // 类上面的校验策略
        TableEntity tableEntity = entity.findAnnotation(TableEntity.class);
        if (tableEntity != null && tableEntity.validationPolicy() != ValidationPolicy.NOT_CHECK) {
            validationPolicy = tableEntity.validationPolicy();
        }
        // 字段上的校验策略
        TableColumn tableColumn = property.findAnnotation(TableColumn.class);
        if (tableColumn != null && tableColumn.validationPolicy() != ValidationPolicy.NOT_CHECK) {
            validationPolicy = tableColumn.validationPolicy();
        }
        // 使用策略判定字段是否有效
        return validationPolicy.isEffective(value);
    }

    private void writeNullInternal(OutboundRow sink, RelationalPersistentProperty property) {
        sink.put(property.getColumnName(), Parameter.empty(getPotentiallyConvertedSimpleNullType(property.getType())));
    }

    private Class<?> getPotentiallyConvertedSimpleNullType(Class<?> type) {

        Optional<Class<?>> customTarget = getConversions().getCustomWriteTarget(type);

        if (customTarget.isPresent()) {
            return customTarget.get();

        }

        if (type.isEnum()) {
            return String.class;
        }

        return type;
    }

    /**
     * Checks whether we have a custom conversion registered for the given value into an arbitrary simple type. Returns
     * the converted value if so. If not, we perform special enum handling or simply return the value as is.
     *
     * @param value value
     * @return object
     */
    @Nullable
    private Object getPotentiallyConvertedSimpleWrite(@Nullable Object value) {
        return getPotentiallyConvertedSimpleWrite(value, Object.class);
    }

    /**
     * Checks whether we have a custom conversion registered for the given value into an arbitrary simple type. Returns
     * the converted value if so. If not, we perform special enum handling or simply return the value as is.
     *
     * @param value value
     * @return object
     */
    @Nullable
    @SuppressWarnings("SameParameterValue")
    private Object getPotentiallyConvertedSimpleWrite(@Nullable Object value, Class<?> typeHint) {

        if (value == null) {
            return null;
        }

        if (Object.class != typeHint) {
            if (getConversionService().canConvert(value.getClass(), typeHint)) {
                value = getConversionService().convert(value, typeHint);
            }
        }

        assert value != null;
        Optional<Class<?>> customTarget = getConversions().getCustomWriteTarget(value.getClass());

        if (customTarget.isPresent()) {
            return getConversionService().convert(value, customTarget.get());
        }

        return Enum.class.isAssignableFrom(value.getClass()) ? ((Enum<?>) value).name() : value;
    }


    private <R> RelationalPersistentEntity<R> getRequiredPersistentEntity(Class<R> type) {
        return (RelationalPersistentEntity<R>) getMappingContext().getRequiredPersistentEntity(type);
    }

    private String getPropertyName(RelationalPersistentProperty property) {
        return ((R2dbcMappingContext) this.getMappingContext()).getNamingStrategy().getColumnName(property);
    }

    /**
     * Returns given object as {@link Collection}. Will return the {@link Collection} as is if the source is a
     * {@link Collection} already, will convert an array into a {@link Collection} or simply create a single element
     * collection for everything else.
     *
     * @param source source data
     * @return collection
     */
    private static Collection<?> asCollection(Object source) {

        if (source instanceof Collection) {
            return (Collection<?>) source;
        }

        return source.getClass().isArray() ? CollectionUtils.arrayToList(source) : Collections.singleton(source);
    }

    enum NoOpParameterValueProvider implements ParameterValueProvider<RelationalPersistentProperty> {

        INSTANCE;

        @Override
        public <T> T getParameterValue(
                org.springframework.data.mapping.Parameter<T, RelationalPersistentProperty> parameter) {
            return null;
        }
    }

    private class RowParameterValueProvider implements ParameterValueProvider<RelationalPersistentProperty> {

        private final Row resultSet;
        @Nullable
        private final RowMetadata metadata;
        private final RelationalPersistentEntity<?> entity;
        private final RelationalConverter converter;
        private final String prefix;

        public RowParameterValueProvider(Row resultSet, @Nullable RowMetadata metadata, RelationalPersistentEntity<?> entity,
                                         RelationalConverter converter, String prefix) {
            this.resultSet = resultSet;
            this.metadata = metadata;
            this.entity = entity;
            this.converter = converter;
            this.prefix = prefix;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mapping.model.ParameterValueProvider#getParameterValue(org.springframework.data.mapping.PreferredConstructor.Parameter)
         */
        @Override
        @Nullable
        public <T> T getParameterValue(
                org.springframework.data.mapping.Parameter<T, RelationalPersistentProperty> parameter) {

            RelationalPersistentProperty property = this.entity.getRequiredPersistentProperty(Objects.requireNonNull(parameter.getName()));
            Object value = readFrom(this.resultSet, this.metadata, property, this.prefix);

            if (value == null) {
                return null;
            }

            Class<T> type = parameter.getType().getType();

            if (type.isInstance(value)) {
                return type.cast(value);
            }

            try {
                return this.converter.getConversionService().convert(value, type);
            } catch (Exception o_O) {
                throw new MappingException(String.format("Couldn't read parameter %s.", parameter.getName()), o_O);
            }
        }
    }
}
