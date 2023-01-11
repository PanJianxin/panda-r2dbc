package com.jxpanda.r2dbc.spring.data.extension.annotation;

import com.jxpanda.r2dbc.spring.data.extension.constant.DateTimeConstant;
import com.jxpanda.r2dbc.spring.data.extension.support.EnvironmentKit;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.function.Supplier;

import static java.lang.annotation.ElementType.*;

/**
 * 逻辑删除标记字段
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {FIELD, METHOD, ANNOTATION_TYPE})
public @interface TableLogic {

    /**
     * 是否开启逻辑删除
     * 字段上的配置优先级是最高的
     * 这个配置的意义在于，支持在全局配置打开\关闭逻辑删除的情况下啊
     * 独立控制当前对象的逻辑删除是否开启
     */
    boolean enable() default true;

    /**
     * 正常情况下的值「数据未删除」
     * 默认逻辑未删除值（该值可无、会自动获取全局配置）
     * 默认情况下，只要字段的值等于「undeleteValue」则表示该字段「未被删除」
     */
    Value undeleteValue();

    /**
     * 被删除的值「数据已删除」
     * 默认逻辑删除值（该值可无、会自动获取全局配置）
     * 默认情况下，只要字段的值不等于「undeleteValue」则表示该字段「被删除」
     */
    Value deleteValue();


    /**
     * 注解里的成员变量类型范围有限，只能用几个常用的类型和枚举
     * 所以要实现能自定义设置值，特别是需要动态取值（例如获取当前时间）的时候，用枚举是一个不错的方法
     * 这里枚举了常用的逻辑删除值
     * 以及两个自定义设置的值，两个自定义的值可以通过配置文件配置
     */
    @Getter
    @AllArgsConstructor
    enum Value {

        /**
         * null
         */
        NULL(() -> null),

        /**
         * 0
         */
        ZERO(() -> 0),
        /**
         * 1
         */
        ONE(() -> 1),

        /**
         * false
         */
        FALSE(() -> false),
        /**
         * true
         */
        TRUE(() -> true),

        /**
         * 1970-01-01 00:00:00
         */
        DATE_1970(() -> DateTimeConstant.DATE_1970_01_01_00_00_00),
        /**
         * now
         */
        DATE_NOW(Date::new),

        /**
         * 1970-01-01 00:00:00
         */
        DATETIME_1970(() -> DateTimeConstant.DATETIME_1970_01_01_00_00_00),
        /**
         * now
         */
        DATETIME_NOW(LocalDateTime::now),

        /**
         * 自定义值，从配置文件取值
         */
        CUSTOMER_UNDELETE(() -> ValueConstant.UNDELETE),
        /**
         * 自定义值，从配置文件取值
         */
        CUSTOMER_DELETE(() -> ValueConstant.DELETE);


        private final Supplier<Object> supplier;

    }

    final class ValueConstant {

        private static final String PROPERTIES_KEY_UNDELETE_VALUE = "panda.r2dbc.logic-delete.undelete-value";
        private static final String PROPERTIES_KEY_DELETE_VALUE = "panda.r2dbc.logic-delete.delete-value";

        private static final Object UNDELETE;
        private static final Object DELETE;

        static {
            UNDELETE = EnvironmentKit.getOrDefault(PROPERTIES_KEY_UNDELETE_VALUE, "", Object.class);
            DELETE = EnvironmentKit.getOrDefault(PROPERTIES_KEY_DELETE_VALUE, "", Object.class);
        }

    }

}
