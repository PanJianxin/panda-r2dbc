package com.jxpanda.r2dbc.spring.data.extension.annotation;

import org.springframework.data.annotation.Id;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

@Id
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {FIELD, METHOD, ANNOTATION_TYPE})
public @interface TableId {

    /**
     * 字段名（可无）
     */
    String name() default "";



}
