package com.jxpanda.r2dbc.spring.data.mapping.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.data.relational.core.mapping.Column;

import java.lang.annotation.*;

/**
 * @see Column
 * 原spring的Column注解只提供了一个value字段，这里需要做一些扩展，更全面的描述字段
 * 例如：虚拟字段，数据库中可能不存在，Json类型，期望ORM框架能自动把序列化/反序列化的工作做掉
 * */
@Column
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface TableColumn {

    @AliasFor(annotation = Column.class, attribute = "value")
    String value() default "";

    boolean exists() default true;

    boolean isJson() default false;

    String alias() default "";

}
