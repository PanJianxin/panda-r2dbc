//package com.jxpanda.r2dbc.spring.data.core.kit;
//
//import jakarta.annotation.PostConstruct;
//import lombok.AllArgsConstructor;
//import org.springframework.dao.InvalidDataAccessResourceUsageException;
//import org.springframework.data.r2dbc.convert.R2dbcConverter;
//import org.springframework.data.r2dbc.dialect.R2dbcDialect;
//import org.springframework.data.r2dbc.mapping.OutboundRow;
//import org.springframework.data.r2dbc.support.ArrayUtils;
//import org.springframework.data.relational.core.dialect.ArrayColumns;
//import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
//import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
//import org.springframework.r2dbc.core.Parameter;
//import org.springframework.stereotype.Component;
//import org.springframework.util.Assert;
//import org.springframework.util.ClassUtils;
//import org.springframework.util.CollectionUtils;
//
//import java.util.Collection;
//
//@SuppressWarnings("deprecation")
//@Component
//@AllArgsConstructor
//public class DataAccessKit {
//
//    private final R2dbcDialect r2dbcDialect;
//    private final R2dbcConverter r2dbcConverter;
//
//    private static R2dbcDialect staticR2dbcDialect;
//    private static R2dbcConverter staticR2dbcConverter;
//
//    @PostConstruct
//    private void init() {
//        staticR2dbcDialect = r2dbcDialect;
//        staticR2dbcConverter = r2dbcConverter;
//    }
//
//    public static OutboundRow getOutboundRow(Object object) {
//
//        Assert.notNull(object, "Entity object must not be null");
//
//        OutboundRow row = new OutboundRow();
//
//        staticR2dbcConverter.write(object, row);
//
//        RelationalPersistentEntity<?> entity = R2dbcMappingKit.getPersistentEntity(ClassUtils.getUserClass(object));
//
//        for (RelationalPersistentProperty property : entity) {
//
//            Parameter value = row.get(property.getColumnName());
//            if (value != null && shouldConvertArrayValue(property, value)) {
//
//                Parameter writeValue = getArrayValue(value, property);
//                row.put(property.getColumnName(), writeValue);
//            }
//        }
//
//        return row;
//    }
//
//    private static boolean shouldConvertArrayValue(RelationalPersistentProperty property, Parameter value) {
//
//        if (!property.isCollectionLike()) {
//            return false;
//        }
//
//        if (value.hasValue() && (value.getValue() instanceof Collection || value.getValue().getClass().isArray())) {
//            return true;
//        }
//
//        if (Collection.class.isAssignableFrom(value.getType()) || value.getType().isArray()) {
//            return true;
//        }
//
//        return false;
//    }
//
//    private static Parameter getArrayValue(Parameter value, RelationalPersistentProperty property) {
//
//        if (value.getType().equals(byte[].class)) {
//            return value;
//        }
//
//        ArrayColumns arrayColumns = staticR2dbcDialect.getArraySupport();
//
//        if (!arrayColumns.isSupported()) {
//            throw new InvalidDataAccessResourceUsageException(
//                    "Dialect " + staticR2dbcDialect.getClass().getName() + " does not support array columns");
//        }
//
//        Class<?> actualType = null;
//        if (value.getValue() instanceof Collection) {
//            actualType = CollectionUtils.findCommonElementType((Collection<?>) value.getValue());
//        } else if (!value.isEmpty() && value.getValue().getClass().isArray()) {
//            actualType = value.getValue().getClass().getComponentType();
//        }
//
//        if (actualType == null) {
//            actualType = property.getActualType();
//        }
//
//        actualType = staticR2dbcConverter.getTargetType(actualType);
//
//        if (value.isEmpty()) {
//
//            Class<?> targetType = arrayColumns.getArrayType(actualType);
//            int depth = actualType.isArray() ? ArrayUtils.getDimensionDepth(actualType) : 1;
//            Class<?> targetArrayType = ArrayUtils.getArrayClass(targetType, depth);
//            return Parameter.empty(targetArrayType);
//        }
//
//        return Parameter.fromOrEmpty(staticR2dbcConverter.getArrayValue(arrayColumns, property, value.getValue()),
//                actualType);
//    }
//
//}
