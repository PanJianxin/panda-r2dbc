package com.jxpanda.r2dbc.spring.data.core.convert;

import com.jxpanda.r2dbc.spring.data.core.enhance.handler.R2dbcCustomTypeHandlers;
import lombok.Getter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.SpELExpressionEvaluator;
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter;
import org.springframework.data.relational.core.conversion.RowDocumentAccessor;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Panda
 */
@Getter
public class MappingReactiveConverter extends MappingR2dbcConverter {

    private final R2dbcCustomTypeHandlers typeHandlers;


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
            R2dbcCustomTypeHandlers typeHandlers) {
        super(context, conversions);
        this.typeHandlers = typeHandlers;
    }

    @Override
    protected RelationalPropertyValueProvider newValueProvider(RowDocumentAccessor documentAccessor, SpELExpressionEvaluator evaluator, ConversionContext context) {
        return new RelationalPropertyValueProviderDecorator((DocumentValueProvider) super.newValueProvider(documentAccessor, evaluator, context), getTypeHandlers());
    }


    /**
     * 为关系型数据库属性值提供装饰器，能够处理特定类型的值转换。
     *
     * @param originalValueProvider 原始属性值提供者，负责基础的属性值获取。
     * @param typeHandlers          自定义类型处理器，用于处理特定类型的值的读取。
     */
    private record RelationalPropertyValueProviderDecorator(DocumentValueProvider originalValueProvider,
                                                            R2dbcCustomTypeHandlers typeHandlers) implements RelationalPropertyValueProvider {

        /**
         * 检查给定的属性是否有值。
         *
         * @param property 要检查的属性。
         * @return 如果属性有值，则返回true；否则返回false。
         */
        @Override
        public boolean hasValue(RelationalPersistentProperty property) {
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
            return new RelationalPropertyValueProviderDecorator(valueProvider, typeHandlers);
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
            // 检查是否有针对当前属性的类型处理器
            if (typeHandlers.hasTypeHandler(property)) {
                Object value = originalValueProvider.accessor().get(property);
                // 断言，值不会为null， 因为前置的hasValue方法已经检查过属性是否有值
                Assert.notNull(value, "Value must be not null!");
                // 使用类型处理器读取和转换属性值
                return (T) typeHandlers.read(value, property);
            }
            // 如果没有特殊处理器，直接从原始提供者获取值
            return originalValueProvider.getPropertyValue(property);
        }
    }


}
