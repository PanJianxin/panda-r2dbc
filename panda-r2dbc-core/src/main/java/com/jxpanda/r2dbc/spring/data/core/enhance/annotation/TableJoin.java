package com.jxpanda.r2dbc.spring.data.core.enhance.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.sql.Join;

import java.lang.annotation.*;

@Table
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TableJoin {

    /**
     * 左表表名
     * */
    @AliasFor(annotation = Table.class, attribute = "name")
    String leftTable();

    /**
     * 右表表名
     * */
    String rightTable();

    /**
     * join类型
     * */
    Join.JoinType joinType() default Join.JoinType.JOIN;

    /**
     * on表达式语句
     * */
    String on();

}
