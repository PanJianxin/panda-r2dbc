package com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker.domain;

import com.jxpanda.r2dbc.spring.data.core.enhance.query.criteria.EnhancedCriteria;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.BiFunction;

@Getter
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
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
    IN(EnhancedCriteria.EnhancedCriteriaStep::in),
    NOT_IN(EnhancedCriteria.EnhancedCriteriaStep::notIn),
    LIKE(EnhancedCriteria.EnhancedCriteriaStep::like),
    BETWEEN((criteriaStep, params) -> {
        List<Object> objects = (List<Object>) params;
        return criteriaStep.between(objects.get(0), objects.get(1));
    });

    private final BiFunction<EnhancedCriteria.EnhancedCriteriaStep, Object, EnhancedCriteria> function;
}
