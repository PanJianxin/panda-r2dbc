package com.jxpanda.r2dbc.spring.data.mapping.annotation;

import com.jxpanda.r2dbc.spring.data.mapping.policy.NullPolicy;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.relational.core.mapping.Table;

import java.lang.annotation.*;

/**
 * @see Table
 * 原spring的Table注解只提供了一个name、schema，这里需要做一些扩展，更全面的描述字段
 * 例如：虚拟字段，数据库中可能不存在，Json类型，期望ORM框架能自动把序列化/反序列化的工作做掉
 * 是否做空安全处理等
 */
@Table
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TableEntity {

    /**
     * 与原Table注解中的name字段映射，原Table注解中，name和value相互映射
     */
    @AliasFor(annotation = Table.class, attribute = "name")
    String name() default "";

    /**
     * 与原Table注解中的schema字段映射
     */
    @AliasFor(annotation = Table.class, attribute = "schema")
    String schema() default "";

    /**
     * 别名
     * 支持 SELECT XXX AS「ALIAS」的语法
     */
    String alias() default "";

    /**
     * 表前缀，默认没有
     */
    String prefix() default "";

    /**
     * 空值处理策略
     * 实体内的null值处理策略，默认NULLABLE
     * 即：全都允许为空
     * 优先级小于TableColumn注解上的策略
     */
    NullPolicy nullPolicy() default NullPolicy.NULLABLE;


}
