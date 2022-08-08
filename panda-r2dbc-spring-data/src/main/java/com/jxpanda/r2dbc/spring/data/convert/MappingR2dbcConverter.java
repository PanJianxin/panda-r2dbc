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
package com.jxpanda.r2dbc.spring.data.convert;

import com.jxpanda.r2dbc.spring.data.mapping.OutboundRow;
import com.jxpanda.r2dbc.spring.data.support.ArrayUtils;
import io.r2dbc.spi.ColumnMetadata;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;

import java.util.*;
import java.util.function.BiFunction;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mapping.*;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.data.mapping.model.DefaultSpELExpressionEvaluator;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.mapping.model.SpELContext;
import org.springframework.data.mapping.model.SpELExpressionEvaluator;
import org.springframework.data.mapping.model.SpELExpressionParameterValueProvider;
import org.springframework.data.relational.core.conversion.BasicRelationalConverter;
import org.springframework.data.relational.core.conversion.RelationalConverter;
import org.springframework.data.relational.core.dialect.ArrayColumns;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * Converter for R2DBC.
 *
 * @author Mark Paluch
 * @author Oliver Drotbohm
 */
//@SuppressWarnings("DuplicatedCode")
public class MappingR2dbcConverter extends BasicRelationalConverter implements R2dbcConverter {

    /**
     * Creates a new {@link MappingR2dbcConverter} given {@link MappingContext}.
     *
     * @param context must not be {@literal null}.
     */
    public MappingR2dbcConverter(
            MappingContext<? extends RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> context) {
        super(context, new R2dbcCustomConversions(R2dbcCustomConversions.STORE_CONVERSIONS, Collections.emptyList()));
    }

    /**
     * Creates a new {@link MappingR2dbcConverter} given {@link MappingContext} and {@link CustomConversions}.
     *
     * @param context must not be {@literal null}.
     */
    public MappingR2dbcConverter(
            MappingContext<? extends RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> context,
            CustomConversions conversions) {
        super(context, conversions);
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

        TypeInformation<? extends R> typeInfo = ClassTypeInformation.from(type);
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

        String identifier = prefix + property.getColumnName().getReference();

        try {

            Object value = null;
            if (metadata == null || RowMetadataUtils.containsColumn(metadata, identifier)) {
                value = row.get(identifier);
            }

            if (value == null) {
                return null;
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


    public Object readValue(@Nullable Object value, TypeInformation<?> type) {

        if (null == value) {
            return null;
        }

        if (getConversions().hasCustomReadTarget(value.getClass(), type.getType())) {
            return getConversionService().convert(value, type.getType());
        } else if (value instanceof Collection || value.getClass().isArray()) {
            return readCollectionOrArray(asCollection(value), type);
        } else {
            return getPotentiallyConvertedSimpleRead(value, type.getType()).orElse(value);
        }
    }

    /**
     * Reads the given value into a collection of the given {@link TypeInformation}.
     *
     * @param source     must not be {@literal null}.
     * @param targetType must not be {@literal null}.
     * @return the converted {@link Collection} or array, will never be {@literal null}.
     */
    @SuppressWarnings("unchecked")
    private Object readCollectionOrArray(Collection<?> source, TypeInformation<?> targetType) {

        Assert.notNull(source, "Source must not be null!");
        Assert.notNull(targetType, "Target type must not be null!");

        Class<?> collectionType = targetType.isSubTypeOf(Collection.class) //
                ? targetType.getType() //
                : List.class;

        TypeInformation<?> componentType = targetType.getComponentType() != null //
                ? targetType.getComponentType() //
                : ClassTypeInformation.OBJECT;
        Class<?> rawComponentType = componentType.getType();

        Collection<Object> items = targetType.getType().isArray() //
                ? new ArrayList<>(source.size()) //
                : CollectionFactory.createCollection(collectionType, rawComponentType, source.size());

        source.forEach(element -> {
            if (!Object.class.equals(rawComponentType) && element instanceof Collection) {
                if (!rawComponentType.isArray() && !ClassUtils.isAssignable(Iterable.class, rawComponentType)) {
                    throw new MappingException(String.format(
                            "Cannot convert %1$s of type %2$s into an instance of %3$s! Implement a custom Converter<%2$s, %3$s> and register it with the CustomConversions",
                            element, element.getClass(), rawComponentType));
                }
            }
            if (element instanceof List) {
                items.add(readCollectionOrArray((Collection<Object>) element, componentType));
            } else {
                getPotentiallyConvertedSimpleRead(element, rawComponentType).ifPresent(items::add);
            }
        });

        return getPotentiallyConvertedSimpleRead(items, targetType.getType()).orElse(items);

    }

    /**
     * Checks whether we have a custom conversion for the given simple object. Converts the given value if so, applies
     * {@link Enum} handling or returns the value as is.
     *
     * @param value  value
     * @param target must not be {@literal null}.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Optional<Object> getPotentiallyConvertedSimpleRead(@Nullable Object value, Class<?> target) {

        Assert.notNull(target, "Target class must not be null.");

        if (value == null || ClassUtils.isAssignableValue(target, value)) {
            return Optional.ofNullable(value);
        }

        if (getConversions().hasCustomReadTarget(value.getClass(), target)) {
            return Optional.ofNullable(getConversionService().convert(value, target));
        }

        if (Enum.class.isAssignableFrom(target)) {
            return Optional.of(Enum.valueOf((Class<Enum>) target, value.toString()));
        }

        return Optional.ofNullable(getConversionService().convert(value, target));
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

//        PreferredConstructor<S, RelationalPersistentProperty> persistenceConstructor = entity.getPersistenceConstructor();
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
    public void write(Object source, OutboundRow sink) {

        Class<?> userClass = ClassUtils.getUserClass(source);

        Optional<Class<?>> customTarget = getConversions().getCustomWriteTarget(userClass, OutboundRow.class);
        if (customTarget.isPresent()) {

            OutboundRow result = getConversionService().convert(source, OutboundRow.class);
            if (result != null) {
                sink.putAll(result);
            }
            return;
        }

        writeInternal(source, sink, userClass);
    }

    private void writeInternal(Object source, OutboundRow sink, Class<?> userClass) {

        RelationalPersistentEntity<?> entity = getRequiredPersistentEntity(userClass);
        PersistentPropertyAccessor<?> propertyAccessor = entity.getPropertyAccessor(source);

        writeProperties(sink, entity, propertyAccessor, entity.isNew(source));
    }

    private void writeProperties(OutboundRow sink, RelationalPersistentEntity<?> entity,
                                 PersistentPropertyAccessor<?> accessor, boolean isNew) {

        for (RelationalPersistentProperty property : entity) {

            if (!property.isWritable()) {
                continue;
            }

            Object value;

            if (property.isIdProperty()) {
                IdentifierAccessor identifierAccessor = entity.getIdentifierAccessor(accessor.getBean());
                value = identifierAccessor.getIdentifier();
            } else {
                value = accessor.getProperty(property);
            }

            if (value == null) {
                writeNullInternal(sink, property);
                continue;
            }

            if (getConversions().isSimpleType(value.getClass())) {
                writeSimpleInternal(sink, value, isNew, property);
            } else {
                writePropertyInternal(sink, value, isNew, property);
            }
        }
    }

    @SuppressWarnings("unused")
    private void writeSimpleInternal(OutboundRow sink, Object value, boolean isNew,
                                     RelationalPersistentProperty property) {

        Object result = getPotentiallyConvertedSimpleWrite(value);

        sink.put(property.getColumnName(),
                Parameter.fromOrEmpty(result, getPotentiallyConvertedSimpleNullType(property.getType())));
    }

    private void writePropertyInternal(OutboundRow sink, Object value, boolean isNew,
                                       RelationalPersistentProperty property) {

        TypeInformation<?> valueType = ClassTypeInformation.from(value.getClass());

        if (valueType.isCollectionLike()) {

            if (valueType.getActualType() != null && valueType.getRequiredActualType().isCollectionLike()) {

                // pass-thru nested collections
                writeSimpleInternal(sink, value, isNew, property);
                return;
            }

            List<Object> collectionInternal = createCollection(asCollection(value), property);
            sink.put(property.getColumnName(), Parameter.from(collectionInternal));
            return;
        }

        throw new InvalidDataAccessApiUsageException("Nested entities are not supported");
    }

    /**
     * Writes the given {@link Collection} using the given {@link RelationalPersistentProperty} information.
     *
     * @param collection must not be {@literal null}.
     * @param property   must not be {@literal null}.
     * @return object list
     */
    protected List<Object> createCollection(Collection<?> collection, RelationalPersistentProperty property) {
        return writeCollectionInternal(collection, property.getTypeInformation(), new ArrayList<>());
    }

    /**
     * Populates the given {@link Collection sink} with converted values from the given {@link Collection source}.
     *
     * @param source the collection to create a {@link Collection} for, must not be {@literal null}.
     * @param type   the {@link TypeInformation} to consider or {@literal null} if unknown.
     * @param sink   the {@link Collection} to write to.
     * @return object list
     */
    @SuppressWarnings("unchecked")
    private List<Object> writeCollectionInternal(Collection<?> source, @Nullable TypeInformation<?> type,
                                                 Collection<?> sink) {

        TypeInformation<?> componentType = null;

        List<Object> collection = sink instanceof List ? (List<Object>) sink : new ArrayList<>(sink);

        if (type != null) {
            componentType = type.getComponentType();
        }

        for (Object element : source) {

            Class<?> elementType = element == null ? null : element.getClass();

            if (elementType == null || getConversions().isSimpleType(elementType)) {
                collection.add(getPotentiallyConvertedSimpleWrite(element,
                        componentType != null ? componentType.getType() : Object.class));
            } else if (element instanceof Collection || elementType.isArray()) {
                collection.add(writeCollectionInternal(asCollection(element), componentType, new ArrayList<>()));
            } else {
                throw new InvalidDataAccessApiUsageException("Nested entities are not supported");
            }
        }

        return collection;
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
    @SuppressWarnings("ConstantConditions")
    @Nullable
    private Object getPotentiallyConvertedSimpleWrite(@Nullable Object value, Class<?> typeHint) {

        if (value == null) {
            return null;
        }

        if (Object.class != typeHint) {

            if (getConversionService().canConvert(value.getClass(), typeHint)) {
                value = getConversionService().convert(value, typeHint);
            }
        }

        Optional<Class<?>> customTarget = getConversions().getCustomWriteTarget(value.getClass());

        if (customTarget.isPresent()) {
            return getConversionService().convert(value, customTarget.get());
        }

        return Enum.class.isAssignableFrom(value.getClass()) ? ((Enum<?>) value).name() : value;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.convert.R2dbcConverter#getArrayValue(org.springframework.data.r2dbc.dialect.ArrayColumns, org.springframework.data.relational.core.mapping.RelationalPersistentProperty, java.lang.Object)
     */
    @SuppressWarnings("ConstantConditions")
    @Override
    public Object getArrayValue(ArrayColumns arrayColumns, RelationalPersistentProperty property, Object value) {

        Class<?> actualType = null;
        if (value instanceof Collection) {
            actualType = CollectionUtils.findCommonElementType((Collection<?>) value);
        } else if (value.getClass().isArray()) {
            actualType = value.getClass().getComponentType();
        }

        if (actualType == null) {
            actualType = property.getActualType();
        }

        Class<?> targetType = arrayColumns.getArrayType(actualType);

        if (!property.isArray() || !targetType.isAssignableFrom(value.getClass())) {

            int depth = value.getClass().isArray() ? ArrayUtils.getDimensionDepth(value.getClass()) : 1;
            Class<?> targetArrayType = ArrayUtils.getArrayClass(targetType, depth);
            return getConversionService().convert(value, targetArrayType);
        }

        return value;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.convert.R2dbcConverter#getTargetType(Class)
     */
    @Override
    public Class<?> getTargetType(Class<?> valueType) {

        Optional<Class<?>> writeTarget = getConversions().getCustomWriteTarget(valueType);

        return writeTarget.orElseGet(() -> {
            return Enum.class.isAssignableFrom(valueType) ? String.class : valueType;
        });
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.convert.R2dbcConverter#isSimpleType(Class)
     */
    @Override
    public boolean isSimpleType(Class<?> type) {
        return getConversions().isSimpleType(type);
    }

    // ----------------------------------
    // Id handling
    // ----------------------------------

    /**
     * Returns a {@link java.util.function.Function} that populates the id property of the {@code object} from a
     * {@link Row}.
     *
     * @param object must not be {@literal null}.
     * @return result
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> BiFunction<Row, RowMetadata, T> populateIdIfNecessary(T object) {

        Assert.notNull(object, "Entity object must not be null!");

        Class<?> userClass = ClassUtils.getUserClass(object);
        RelationalPersistentEntity<?> entity = getMappingContext().getRequiredPersistentEntity(userClass);

        if (!entity.hasIdProperty()) {
            return (row, rowMetadata) -> object;
        }

        return (row, metadata) -> {

            PersistentPropertyAccessor<?> propertyAccessor = entity.getPropertyAccessor(object);
            RelationalPersistentProperty idProperty = entity.getRequiredIdProperty();

            boolean idPropertyUpdateNeeded;

            Object id = propertyAccessor.getProperty(idProperty);
            if (idProperty.getType().isPrimitive()) {
                idPropertyUpdateNeeded = id instanceof Number && ((Number) id).longValue() == 0;
            } else {
                idPropertyUpdateNeeded = id == null;
            }

            if (idPropertyUpdateNeeded) {
                return potentiallySetId(row, metadata, propertyAccessor, idProperty) //
                        ? (T) propertyAccessor.getBean() //
                        : object;
            }

            return object;
        };
    }

    private boolean potentiallySetId(Row row, RowMetadata metadata, PersistentPropertyAccessor<?> propertyAccessor,
                                     RelationalPersistentProperty idProperty) {

        String idColumnName = idProperty.getColumnName().getReference();
        Object generatedIdValue = extractGeneratedIdentifier(row, metadata, idColumnName);

        if (generatedIdValue == null) {
            return false;
        }

        ConversionService conversionService = getConversionService();
        propertyAccessor.setProperty(idProperty, conversionService.convert(generatedIdValue, idProperty.getType()));

        return true;
    }

    @Nullable
    private Object extractGeneratedIdentifier(Row row, RowMetadata metadata, String idColumnName) {

        if (RowMetadataUtils.containsColumn(metadata, idColumnName)) {
            return row.get(idColumnName);
        }

        Iterable<? extends ColumnMetadata> columns = metadata.getColumnMetadatas();
        Iterator<? extends ColumnMetadata> it = columns.iterator();

        if (it.hasNext()) {
            ColumnMetadata column = it.next();
            return row.get(column.getName());
        }

        return null;
    }

    private <R> RelationalPersistentEntity<R> getRequiredPersistentEntity(Class<R> type) {
        return (RelationalPersistentEntity<R>) getMappingContext().getRequiredPersistentEntity(type);
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
