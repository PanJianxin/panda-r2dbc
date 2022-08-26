package com.jxpanda.r2dbc.spring.data.mapping.annotation;

import com.jxpanda.r2dbc.spring.data.mapping.policy.NullPolicy;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.relational.core.mapping.Column;

import java.lang.annotation.*;

/**
 * @see Column
 * 原spring的Column注解只提供了一个value字段，这里需要做一些扩展，更全面的描述字段
 * 例如：虚拟字段，数据库中可能不存在，Json类型，期望ORM框架能自动把序列化/反序列化的工作做掉
 */
@Column
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface TableColumn {

    /**
     * 字段名
     */
    @AliasFor(annotation = Column.class, attribute = "value")
    String name() default "";

    /**
     * 别名，SELECT XXX AS「ALIAS」
     * 这个配置有值的话，会生成如上SQL语句
     */
    String alias() default "";

    /**
     * 字段前缀
     */
    String prefix() default "";

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
     * 空值处理策略
     * 字段null值处理策略，默认NULLABLE
     * 即：全都允许为空
     * 优先级大于TableColumn注解上的策略
     */
    NullPolicy nullPolicy() default NullPolicy.NULLABLE;

}
