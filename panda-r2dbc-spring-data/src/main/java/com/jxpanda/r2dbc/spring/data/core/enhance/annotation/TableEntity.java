package com.jxpanda.r2dbc.spring.data.core.enhance.annotation;

import com.jxpanda.r2dbc.spring.data.core.enhance.policy.ValidationPolicy;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.sql.Expression;

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
     * 是否是聚合查询对象
     * 查询对象会直接把对象中的字段解析为表达式，不做多余处理
     * 这个设计目前是一个折中的方案，不得已而为之
     * 主要是想实现简单（单表count或者sum）的聚合查询直接能出结果的效果
     * 这需要利用到SimpleFunction${@link org.springframework.data.relational.core.sql.SimpleFunction}
     * 或者是Expression${@link org.springframework.data.relational.core.sql.Expressions}
     * 所有表达式的解析是在${@link org.springframework.data.r2dbc.query.UpdateMapper#getMappedObject(Expression, RelationalPersistentEntity)}函数中完成的
     * 这个函数有3个分支逻辑
     * 第一段if判断了如果查询所有（select xxx.*）或者没有传递对象则直接返回
     * 后两段限定了只解析
     * <p>
     * ${@link org.springframework.data.relational.core.sql.Column}
     * ${@link org.springframework.data.relational.core.sql.SimpleFunction}
     * 这两个表达式类型
     * </p>
     * 也即是说，普通的${@link org.springframework.data.relational.core.sql.Expressions}表达式
     * 只能「利用」第一段逻辑绕过去（不做额外解析，直接返回原表达式）
     * 因此需要一个判断来主动使entity传递为null，就可以绕过这段逻辑
     */
    boolean isAggregate() default false;

    /**
     * 字段验证策略
     * 优先级列表：字段注解（@TableColumn） > 类注解（@TableEntity） > 全局配置（R2dbcProperty）
     */
    ValidationPolicy validationPolicy() default ValidationPolicy.NOT_CHECK;


}
