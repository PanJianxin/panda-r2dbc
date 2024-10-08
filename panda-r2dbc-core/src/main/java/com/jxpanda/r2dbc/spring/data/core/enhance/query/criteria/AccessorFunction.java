package com.jxpanda.r2dbc.spring.data.core.enhance.query.criteria;

import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableId;
import com.jxpanda.r2dbc.spring.data.infrastructure.constant.StringConstant;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.ReflectionKit;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.StringKit;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * @author Panda
 */
@FunctionalInterface
public interface AccessorFunction<T, R> extends Function<T, R>, Serializable {

    /**
     * 获取字段名
     * @return 字段名
     */
    default String getColumnName() {
        if (AccessorCache.containsKey(this)) {
            return AccessorCache.get(this);
        }

        Field field = ReflectionKit.getFieldFromAccessorFunction(this);
        String columnName = StringConstant.BLANK;
        TableId tableId = field.getAnnotation(TableId.class);
        if (tableId != null) {
            columnName = tableId.name();
        }

        if (StringKit.isBlank(columnName)) {
            TableColumn tableColumn = field.getAnnotation(TableColumn.class);
            if (tableColumn != null) {
                columnName = tableColumn.name();
            }
        }

        columnName = StringKit.isBlank(columnName) ? field.getName() : columnName;
        AccessorCache.put(this, columnName);

        return columnName;
    }

}
