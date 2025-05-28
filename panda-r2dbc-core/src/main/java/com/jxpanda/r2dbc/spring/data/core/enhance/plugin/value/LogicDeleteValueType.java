package com.jxpanda.r2dbc.spring.data.core.enhance.plugin.value;

import com.jxpanda.r2dbc.spring.data.config.R2dbcEnvironment;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.criteria.EnhancedCriteria;
import com.jxpanda.r2dbc.spring.data.infrastructure.constant.DateTimeConstant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.relational.core.query.Criteria;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 注解里的成员变量类型范围有限，只能用几个常用的类型和枚举
 * 所以要实现能自定义设置值，特别是需要动态取值（例如获取当前时间）的时候，用枚举是一个不错的方法
 * 这里枚举了常用的逻辑删除值
 * 以及两个自定义设置的值，两个自定义的值可以通过配置文件配置
 */
@Getter
@RequiredArgsConstructor
public enum LogicDeleteValueType {

    NUMBER(
            () -> 1,
            () -> 0,
            Criteria.CriteriaStep::is
    ),
    BOOLEAN(
            () -> true,
            () -> false,
            Criteria.CriteriaStep::is
    ),
    DATE_1970(
            Date::new,
            () -> DateTimeConstant.DATE_1970_01_01_00_00_00,
            Criteria.CriteriaStep::is
    ),
    DATE_9999(
            Date::new,
            Date::new,
            Criteria.CriteriaStep::greaterThan
    ),
    DATE_TIME_1970(
            LocalDateTime::now,
            () -> DateTimeConstant.DATETIME_1970_01_01_00_00_00,
            Criteria.CriteriaStep::is
    ),
    DATE_TIME_9999(
            LocalDateTime::now,
            LocalDateTime::now,
            Criteria.CriteriaStep::greaterThan
    ),
    USE_PROPERTIES(
            () -> ValueConstant.DELETE,
            () -> ValueConstant.UNDELETE,
            Criteria.CriteriaStep::is
    ),
    CUSTOMER(
            () -> null,
            () -> null,
            Criteria.CriteriaStep::is
    );

    private final Supplier<Object> deleteValue;
    private final Supplier<Object> undeleteValue;
    private final BiFunction<Criteria.CriteriaStep, Object, Criteria> criteriaFunction;

    public Criteria createCriteria(Criteria.CriteriaStep criteriaStep, Object value) {
        return criteriaFunction.apply(criteriaStep, value);
    }


    static final class ValueConstant {

        private static final Object DELETE;
        private static final Object UNDELETE;

        static {
            LogicDeleteValue logicDeleteValue = R2dbcEnvironment.getLogicDeleteProperties().value();
            DELETE = logicDeleteValue.getDeleteValue();
            UNDELETE = logicDeleteValue.getUndeleteValue();
        }

    }

}
