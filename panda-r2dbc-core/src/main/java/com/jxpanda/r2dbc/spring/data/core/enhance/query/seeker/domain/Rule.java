package com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker.domain;

import com.jxpanda.r2dbc.spring.data.infrastructure.constant.StringConstant;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.ReflectionKit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.relational.core.query.Criteria;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

@Getter
@RequiredArgsConstructor
public enum Rule implements RuleFunction {
    /**
     * 等于
     */
    EQ(Criteria.CriteriaStep::is) {
        @Override
        public Criteria nullParamFallback(Criteria.CriteriaStep criteriaStep) {
            return criteriaStep.isNull();
        }
    },
    /**
     * 不等于
     */
    NE(Criteria.CriteriaStep::not) {
        @Override
        public Criteria nullParamFallback(Criteria.CriteriaStep criteriaStep) {
            return criteriaStep.isNotNull();
        }
    },
    IS_NULL((criteriaStep, params) -> criteriaStep.isNull()) {
        @Override
        public boolean allowNull() {
            return true;
        }
    },
    IS_NOT_NULL((criteriaStep, params) -> criteriaStep.isNotNull()) {
        @Override
        public boolean allowNull() {
            return true;
        }
    },
    GT(Criteria.CriteriaStep::greaterThan),
    GE(Criteria.CriteriaStep::greaterThanOrEquals),
    LT(Criteria.CriteriaStep::lessThan),
    LE(Criteria.CriteriaStep::lessThanOrEquals),
    IN(((criteriaStep, params) -> {
        if (params instanceof Collection<?> collection) {
            return criteriaStep.in(collection);
        }
        return criteriaStep.in(params);
    })),
    NOT_IN(((criteriaStep, params) -> {
        if (params instanceof Collection<?> collection) {
            return criteriaStep.notIn(collection);
        }
        return criteriaStep.notIn(params);
    })),
    LIKE((criteriaStep, param) -> criteriaStep.like(StringConstant.PERCENT_SIGN + param + StringConstant.PERCENT_SIGN)),
    BETWEEN((criteriaStep, params) -> {
        List<Object> objects = ReflectionKit.cast(params);
        return criteriaStep.between(objects.get(0), objects.get(1));
    }),
    NOT_BETWEEN((criteriaStep, params) -> {
        List<Object> objects = ReflectionKit.cast(params);
        return criteriaStep.notBetween(objects.get(0), objects.get(1));
    });

    private final BiFunction<Criteria.CriteriaStep, Object, Criteria> function;
}
