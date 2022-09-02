package com.jxpanda.r2dbc.spring.data.extension.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * 逻辑删除标记字段
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {FIELD, METHOD, ANNOTATION_TYPE})
public @interface TableLogic {

    /**
     * 正常情况下的值「数据未删除」
     * 默认逻辑未删除值（该值可无、会自动获取全局配置）
     * 默认情况下，只要字段的值等于「normalValue」则表示该字段「未被删除」
     */
    String normalValue() default "";

    /**
     * 被删除的值「数据已删除」
     * 默认逻辑删除值（该值可无、会自动获取全局配置）
     * 默认情况下，只要字段的值不等于「normalValue」则表示该字段「被删除」
     */
    String deleteValue() default "";


}
