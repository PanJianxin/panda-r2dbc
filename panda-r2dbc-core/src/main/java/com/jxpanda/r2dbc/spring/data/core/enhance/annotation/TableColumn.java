package com.jxpanda.r2dbc.spring.data.core.enhance.annotation;

import com.jxpanda.r2dbc.spring.data.core.enhance.handler.R2dbcTypeHandler;
import com.jxpanda.r2dbc.spring.data.core.enhance.strategy.ValidationStrategy;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.relational.core.mapping.Column;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * @see Column
 * 原spring的Column注解只提供了一个value字段，这里需要做一些扩展，更全面的描述字段
 * 例如：虚拟字段，数据库中可能不存在，Json类型，期望ORM框架能自动把序列化/反序列化的工作做掉
 */
@Column
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {FIELD, ANNOTATION_TYPE})
public @interface TableColumn {

    /**
     * 字段名
     * 会渲染成 ${tableName}.${name}
     * 例如：SELECT ${tableName}.${name}
     * Spring底层源码会强行把表名作为前缀加上去，这会导致某些表达式无法正确的渲染（例如COUNT(*)函数）
     */
    @AliasFor(annotation = Column.class, attribute = "value")
    String name() default "";

    /**
     * 别名，SELECT XXX AS「ALIAS」
     * 这个配置有值的话，会生成如上SQL语句
     */
    String alias() default "";

    /**
     * 函数表达式
     */
    String function() default "";

    /**
     * 字段前缀
     */
    String prefix() default "";

    /**
     * 表名，字段上的表名是为了做join的时候给一个标识
     */
    String fromTable() default "";

    /**
     * entity类型，字段上的表名是为了做join的时候给一个标识
     */
    Class<?> fromEntity() default Object.class;

    /**
     * 属性是否真实存在于表中
     * 针对扩展了Entity的时候，处理不存在与表中的属性使用
     */
    boolean exists() default true;

    /**
     * JSON处理器，用于处理Json类型的数据，默认是不做任何处理
     * 实现以下功能:
     * 1.「读」取数据的时候，自动把「Json字符串」反序列化为对象
     * 2.「写」数据的时候，自动把对象序列化为「Json字符串」
     */
    boolean isJson() default false;

    /**
     * 字段验证策略
     * 优先级列表：字段注解（@TableColumn） > 类注解（@TableEntity） > 全局配置（R2dbcProperty）
     */
    ValidationStrategy validationPolicy() default ValidationStrategy.DEFAULT;


    /**
     * 类型处理器，默认是不处理，一般用于json字段的处理
     */
    Class<? extends R2dbcTypeHandler<Object, Object>> typeHandler() default R2dbcTypeHandler.DefaultHandler.class;

}
