package com.jxpanda.r2dbc.spring.data.core.convert;

import com.jxpanda.r2dbc.spring.data.core.enhance.handler.R2dbcCustomTypeHandlers;
import com.jxpanda.r2dbc.spring.data.core.kit.R2dbcMappingKit;
import lombok.Getter;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.SpELExpressionEvaluator;
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.relational.core.conversion.RowDocumentAccessor;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.domain.RowDocument;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Array;
import java.util.*;

/**
 * 该类继承 {@link MappingR2dbcConverter}
 * 目的是对某些类型进行自定义的转换
 * 运作原理是通过重写 {@link MappingR2dbcConverter#newValueProvider}函数来达到某些类型（Enum、Json）的转换
 * 该函数负责对值进行处理和转换
 *
 * @author Panda
 */
@SuppressWarnings("deprecation")
@Getter
public class MappingReactiveConverter extends MappingR2dbcConverter {

    private final R2dbcCustomTypeHandlers typeHandlers;

    private final NamingStrategy namingStrategy;

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
            NamingStrategy namingStrategy
    ) {
        super(context, conversions);
        this.typeHandlers = typeHandlers;
        this.namingStrategy = namingStrategy;
    }

    /**
     * FIXME: Spring通过这个函数来处理值的读取，因此可以通过增强该函数来植入增强逻辑
     *  增加了typeHandlers的介入，处理typeHandlers所支持的类型
     *  目前typeHandler只支持枚举和json的处理
     * <p>
     * 创建一个新的RelationalPropertyValueProvider实例。
     * 该方法通过使用父类的newValueProvider方法来创建一个基础的DocumentValueProvider实例，然后将其包装在一个RelationalPropertyValueProviderDecorator中，以提供额外的功能或处理。
     *
     * @param documentAccessor 用于访问行文档数据的接口，用于获取和设置文档中的属性值。
     * @param evaluator        SPEL表达式评估器，用于评估表达式并获取值。
     * @param context          转换上下文，提供转换过程中的上下文信息和帮助。
     * @return 返回一个RelationalPropertyValueProviderDecorator实例，该实例装饰了基础的DocumentValueProvider，并提供了类型处理器。
     */
    @Override
    protected RelationalPropertyValueProvider newValueProvider(RowDocumentAccessor documentAccessor, SpELExpressionEvaluator evaluator, ConversionContext context) {
        // 创建并返回一个RelationalPropertyValueProviderDecorator实例，注入类型处理器
        return new RelationalPropertyValueProviderDecorator((DocumentValueProvider) super.newValueProvider(documentAccessor, evaluator, context), getTypeHandlers(), getNamingStrategy());
    }

    // ----------------------------------
    // Entity writing
    // ----------------------------------
    @Override
    @Nullable
    public Object writeValue(@Nullable Object value, TypeInformation<?> type) {

        if (value == null) {
            return null;
        }

        /*
         *  FIXME: 这里的逻辑是后加的，之所以这么做是因为希望实现入参传递枚举，但是数据库存储的是枚举的code的功能
         *      因此在前置逻辑中，已经使用枚举处理器把枚举转为数字类型了
         *      而蛋疼的是，Spring中有一个ConversionService，执行之后又会把数字类型转回枚举类型
         *      所以这里需要判断是枚举的话，就直接返回值
         *      这里是一个临时处理，期望未来能有更优雅的解决方案
         * */
        if (type.getType().isEnum()) {
            return value;
        }

        if (getConversions().isSimpleType(value.getClass())) {

            if (TypeInformation.OBJECT != type && getConversionService().canConvert(value.getClass(), type.getType())) {
                value = getConversionService().convert(value, type.getType());
            }

            return getPotentiallyConvertedSimpleWrite(value);
        }

        if (value.getClass().isArray()) {
            return writeArray(value, type);
        }

        if (value instanceof Collection<?>) {
            return writeCollection((Iterable<?>) value, type);
        }

        RelationalPersistentEntity<?> persistentEntity = getMappingContext().getPersistentEntity(value.getClass());

        if (persistentEntity != null) {

            Object id = persistentEntity.getIdentifierAccessor(value).getIdentifier();
            return writeValue(id, type);
        }

        return getConversionService().convert(value, type.getType());
    }

    private Object writeArray(Object value, TypeInformation<?> type) {

        Class<?> componentType = value.getClass().getComponentType();
        Optional<Class<?>> optionalWriteTarget = getConversions().getCustomWriteTarget(componentType);

        if (optionalWriteTarget.isEmpty() && !componentType.isEnum()) {
            return value;
        }

        Class<?> customWriteTarget = optionalWriteTarget
                .orElseGet(() -> componentType.isEnum() ? String.class : componentType);

        // optimization: bypass identity conversion
        if (customWriteTarget.equals(componentType)) {
            return value;
        }

        TypeInformation<?> component = TypeInformation.OBJECT;
        if (type.isCollectionLike() && type.getActualType() != null) {
            component = type.getRequiredComponentType();
        }

        int length = Array.getLength(value);
        Object target = Array.newInstance(customWriteTarget, length);
        for (int i = 0; i < length; i++) {
            Array.set(target, i, writeValue(Array.get(value, i), component));
        }

        return target;
    }

    @SuppressWarnings("DataFlowIssue")
    private Object writeCollection(Iterable<?> value, TypeInformation<?> type) {

        List<Object> mapped = new ArrayList<>();

        TypeInformation<?> component = TypeInformation.OBJECT;
        if (type.isCollectionLike() && type.getActualType() != null) {
            component = type.getRequiredComponentType();
        }

        for (Object o : value) {
            mapped.add(writeValue(o, component));
        }

        if (type.getType().isInstance(mapped) || !type.isCollectionLike()) {
            return mapped;
        }

        return getConversionService().convert(mapped, type.getType());
    }

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

        RelationalPersistentEntity<?> entity = R2dbcMappingKit.getRequiredEntity(userClass);
        PersistentPropertyAccessor<?> propertyAccessor = entity.getPropertyAccessor(source);

        writeProperties(outboundRow, entity, propertyAccessor, entity.isNew(source));
    }

    private void writeProperties(OutboundRow outboundRow, RelationalPersistentEntity<?> entity,
                                 PersistentPropertyAccessor<?> accessor, boolean isNew) {

        for (RelationalPersistentProperty property : entity) {

            if (!property.isWritable() || !R2dbcMappingKit.isPropertyExists(property)) {
                continue;
            }

            Object value;

            if (property.isIdProperty()) {
                IdentifierAccessor identifierAccessor = entity.getIdentifierAccessor(accessor.getBean());
                value = identifierAccessor.getIdentifier();
            } else if (typeHandlers.hasTypeHandler(property)) {
                /*
                 *  FIXME: 这一个分支是新增逻辑，由于增加了类型处理器，这里需要介入，优先用类型处理器的处理方式
                 * */
                value = typeHandlers.write(accessor.getProperty(property), property);
            } else {
                value = accessor.getProperty(property);
            }

            if (!R2dbcMappingKit.isPropertyEffective(entity, property, value)) {
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

    /**
     * Returns given object as {@link Collection}. Will return the {@link Collection} as is if the source is a
     * {@link Collection} already, will convert an array into a {@link Collection} or simply create a single element
     * collection for everything else.
     *
     * @param source source
     * @return collection
     */
    private static Collection<?> asCollection(Object source) {

        if (source instanceof Collection) {
            return (Collection<?>) source;
        }

        return source.getClass().isArray() ? CollectionUtils.arrayToList(source) : Collections.singleton(source);
    }

    /**
     * 为关系型数据库属性值提供装饰器，能够处理特定类型的值转换。
     *
     * @param originalValueProvider 原始属性值提供者，负责基础的属性值获取。
     * @param typeHandlers          自定义类型处理器，用于处理特定类型的值的读取。
     */
    private record RelationalPropertyValueProviderDecorator(DocumentValueProvider originalValueProvider,
                                                            R2dbcCustomTypeHandlers typeHandlers,
                                                            NamingStrategy namingStrategy
    ) implements RelationalPropertyValueProvider {

        /**
         * 检查给定的属性是否有值。
         *
         * @param property 要检查的属性。
         * @return 如果属性有值，则返回true；否则返回false。
         */
        @Override
        public boolean hasValue(RelationalPersistentProperty property) {
            boolean hasAlias = hasAlias(property);
            if (hasAlias) {
                return getValueWithAlias(property) != null;
            }
            return originalValueProvider.hasValue(property);
        }

        /**
         * 通过提供转换上下文来创建一个新的属性值提供者实例。
         *
         * @param context 转换上下文。
         * @return 一个新的属性值提供者实例，包含了转换上下文。
         */
        @Override
        public RelationalPropertyValueProvider withContext(ConversionContext context) {
            DocumentValueProvider valueProvider = originalValueProvider.withContext(context);
            return new RelationalPropertyValueProviderDecorator(valueProvider, typeHandlers, namingStrategy);
        }

        /**
         * 获取给定属性的值，如果该属性类型需要特殊处理，则使用类型处理器进行转换。
         *
         * @param property 要获取值的属性。
         * @return 属性的值，可能已经经过类型转换。
         */
        @Nullable
        @Override
        @SuppressWarnings("unchecked")
        public <T> T getPropertyValue(RelationalPersistentProperty property) {

            Object value = null;

            // 如果有别名的话，尝试从别名获取值
            if (hasAlias(property)) {
                value = getValueWithAlias(property);
            }

            // 检查是否有针对当前属性的类型处理器
            if (typeHandlers.hasTypeHandler(property)) {
                value = value != null ? value : originalValueProvider.accessor().get(property);
                // 断言，值不会为null， 因为前置的hasValue方法已经检查过属性是否有值
                Assert.notNull(value, "Value must be not null!");
                // 使用类型处理器读取和转换属性值
                return (T) typeHandlers.read(value, property);
            }

            // 如果没有特殊处理，直接从原始提供者获取值
            return value != null ? (T) value : originalValueProvider.getPropertyValue(property);
        }

        /**
         * 检查是否有别名，有的话优先利用别名获取数据，使用getName可以直接获取到别名（没有设定别名则获取字段名）
         * 所以如果字段名和别名不一样，则说明有别名的存在（如果别名的设置和字段名一样，也等于没设置）
         */
        private boolean hasAlias(RelationalPersistentProperty property) {
            String name = getColumnName(property);
            return !name.equals(property.getColumnName().getReference());
        }

        @Nullable
        private Object getValueWithAlias(RelationalPersistentProperty property) {
            RowDocument document = originalValueProvider.accessor().getDocument();
            return document.get(getColumnName(property));
        }

        private String getColumnName(RelationalPersistentProperty property) {
            return namingStrategy.getColumnName(property);
        }

    }


}
