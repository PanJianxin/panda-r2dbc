package com.jxpanda.r2dbc.spring.data.core.enhance.policy;

import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableEntity;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableId;
import com.jxpanda.r2dbc.spring.data.extension.constant.StringConstant;
import lombok.AllArgsConstructor;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.util.ParsingUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 命名策略
 */
@AllArgsConstructor
public enum NamingPolicy implements NamingStrategy {

    /**
     * 默认不做任何处理
     * 使用spring内置的命名策略
     * 阅读源码应该是把camelCase转为了snake_case
     */
    DEFAULT(name -> name),
    /**
     * 下划线分隔命名
     */
    CAMEL_CASE_TO_SNAKE_CASE(NamingPolicy::camelCase2snakeCase),
    /**
     * 驼峰命名
     */
    SNAKE_CASE_TO_CAMEL_CASE(NamingPolicy::snakeCase2camelCase);

    private final Function<String, String> converter;


    @NonNull
    @Override
    public String getSchema() {
        return "";
    }

    @NonNull
    @Override
    public String getTableName(@NonNull Class<?> type) {
        String tableName = null;
        boolean annotationPresent = type.isAnnotationPresent(TableEntity.class);
        if (annotationPresent) {
            TableEntity tableEntity = type.getAnnotation(TableEntity.class);
            tableName = tableEntity.name();
        }
        return ObjectUtils.isEmpty(tableName) ? convert(type.getSimpleName()) : tableName;
    }


    @NonNull
    @Override
    public String getColumnName(@NonNull RelationalPersistentProperty property) {
        String columnName = null;
        boolean isId = property.isAnnotationPresent(TableId.class);
        if (isId) {
            TableId tableId = property.getRequiredAnnotation(TableId.class);
            columnName = tableId.name();
        } else if (property.isAnnotationPresent(TableColumn.class)) {
            TableColumn tableColumn = property.getRequiredAnnotation(TableColumn.class);
            // 如果设置了别名，则返回别名
            columnName = ObjectUtils.isEmpty(tableColumn.alias()) ? tableColumn.name() : tableColumn.alias();
        }
        return ObjectUtils.isEmpty(columnName) ? convert(property.getName()) : columnName;
    }

    public String convert(String string) {
        return this.converter.apply(string);
    }


    /**
     * 把字符串转为snake_case命名规范
     *
     * @param string 待转换字符串
     * @return 转换后的字符串
     */
    public static String camelCase2snakeCase(String string) {
        return String.join(StringConstant.DASH, ParsingUtils.splitCamelCaseToLower(string));
    }

    /**
     * 把字符串转为camelCase命名规范
     *
     * @param string 待转换字符串
     * @return 转换后的字符串
     */
    public static String snakeCase2camelCase(String string) {
        String camelString = Arrays.stream(string.split(StringConstant.DASH))
                .map(StringUtils::capitalize)
                .collect(Collectors.joining(StringConstant.BLANK));
        return StringUtils.uncapitalize(camelString);
    }

}
