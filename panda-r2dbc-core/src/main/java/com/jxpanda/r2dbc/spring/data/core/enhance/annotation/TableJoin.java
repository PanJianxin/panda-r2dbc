package com.jxpanda.r2dbc.spring.data.core.enhance.annotation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.sql.Join;
import org.springframework.data.relational.core.sql.SelectBuilder;
import org.springframework.data.relational.core.sql.TableLike;

import java.lang.annotation.*;
import java.util.function.BiFunction;

@Table
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TableJoin {


    /**
     * 左表表名
     */
    @AliasFor(annotation = Table.class, attribute = "name")
    String leftTable();

    /**
     * 右表表名
     */
    String rightTable();

    /**
     * on表达式语句
     */
    String on();

    /**
     * join类型
     */
    JoinType joinType() default JoinType.JOIN;

    /**
     * 支持的join类型枚举
     */
    @Getter
    @AllArgsConstructor
    enum JoinType {
        JOIN(Join.JoinType.JOIN, SelectBuilder.SelectJoin::join),
        LEFT_JOIN(Join.JoinType.LEFT_OUTER_JOIN, SelectBuilder.SelectJoin::leftOuterJoin);

        private final Join.JoinType joinType;

        private final BiFunction<SelectBuilder.SelectJoin, TableLike, SelectBuilder.SelectOn> function;

    }

}
