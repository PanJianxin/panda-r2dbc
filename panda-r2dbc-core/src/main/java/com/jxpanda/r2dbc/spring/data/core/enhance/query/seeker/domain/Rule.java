package com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker.domain;

import com.jxpanda.r2dbc.spring.data.core.enhance.query.criteria.EnhancedCriteria;
import com.jxpanda.r2dbc.spring.data.infrastructure.constant.StringConstant;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.ReflectionKit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

@Getter
@RequiredArgsConstructor
public enum Rule implements RuleFunction {
    /**
     * 等于
     */
    EQ(EnhancedCriteria.EnhancedCriteriaStep::is) {
        @Override
        public EnhancedCriteria nullParamFallback(EnhancedCriteria.EnhancedCriteriaStep criteriaStep) {
            return criteriaStep.isNull();
        }
    },
    /**
     * 不等于
     */
    NE(EnhancedCriteria.EnhancedCriteriaStep::not) {
        @Override
        public EnhancedCriteria nullParamFallback(EnhancedCriteria.EnhancedCriteriaStep criteriaStep) {
            return criteriaStep.isNotNull();
        }
    },
    GT(EnhancedCriteria.EnhancedCriteriaStep::greaterThan),
    GE(EnhancedCriteria.EnhancedCriteriaStep::greaterThanOrEquals),
    LT(EnhancedCriteria.EnhancedCriteriaStep::lessThan),
    LE(EnhancedCriteria.EnhancedCriteriaStep::lessThanOrEquals),
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

    private final BiFunction<EnhancedCriteria.EnhancedCriteriaStep, Object, EnhancedCriteria> function;
}
