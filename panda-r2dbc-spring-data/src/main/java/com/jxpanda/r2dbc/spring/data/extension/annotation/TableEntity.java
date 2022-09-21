package com.jxpanda.r2dbc.spring.data.extension.annotation;

import com.jxpanda.r2dbc.spring.data.extension.policy.ValidationPolicy;
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
     * 字段验证策略
     * 优先级列表：字段注解（@TableColumn） > 类注解（@TableEntity） > 全局配置（R2dbcProperty）
     */
    ValidationPolicy validationPolicy() default ValidationPolicy.NOT_CHECK;


}
