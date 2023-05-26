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
     * 现在暂时只支持Join和Left outer join两种
     * 因为Spring data中只开放了这两个类型的创建，其他类型虽然在枚举中有
     * 但是，在{@link SelectBuilder.SelectJoin}接口中，并没有支持传递JoinType
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
