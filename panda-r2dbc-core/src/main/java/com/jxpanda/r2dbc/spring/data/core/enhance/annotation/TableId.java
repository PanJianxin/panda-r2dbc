package com.jxpanda.r2dbc.spring.data.core.enhance.annotation;

import com.jxpanda.r2dbc.spring.data.core.enhance.strategy.IdStrategy;
import com.jxpanda.r2dbc.spring.data.core.enhance.strategy.ValidationStrategy;
import org.springframework.data.annotation.Id;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * @author Panda
 */
@Id
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {FIELD})
public @interface TableId {

    /**
     * 字段名（可无）
     */
    String name() default "";

    /**
     * id生成策略
     * 默认是手动处理
     */
    IdStrategy idStrategy() default IdStrategy.DEFAULT;

    /**
     * 校验策略，默认不校验
     */
    ValidationStrategy validationPolicy() default ValidationStrategy.NOT_CHECK;

}
