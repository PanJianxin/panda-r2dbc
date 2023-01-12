package com.jxpanda.r2dbc.spring.data.extension.support;

import com.jxpanda.r2dbc.spring.data.extension.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.extension.annotation.TableId;
import com.jxpanda.r2dbc.spring.data.extension.constant.StringConstant;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@FunctionalInterface
public interface AccessorFunction<T, R> extends Function<T, R>, Serializable {

    Map<AccessorFunction<?, ?>, String> CACHE = new HashMap<>();

    /**
     * 获取字段名
     */
    default String getColumnName() {
        if (CACHE.containsKey(this)) {
            return CACHE.get(this);
        }

        Field field = ReflectionKit.getField(this);
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
        CACHE.put(this, columnName);

        return columnName;
    }

}
