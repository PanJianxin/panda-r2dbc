package com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker.domain;

import com.jxpanda.r2dbc.spring.data.core.enhance.query.criteria.EnhancedCriteria;

import java.util.Collection;
import java.util.function.BiFunction;

public interface RuleFunction {

    BiFunction<EnhancedCriteria.EnhancedCriteriaStep, Object, EnhancedCriteria> getFunction();

    default EnhancedCriteria execute(EnhancedCriteria.EnhancedCriteriaStep criteriaStep, Object param) {
        if (param == null) {
            return nullParamFallback(criteriaStep);
        }
        return getFunction().apply(criteriaStep, param);
    }

    default EnhancedCriteria nullParamFallback(EnhancedCriteria.EnhancedCriteriaStep criteriaStep) {
        throw new UnsupportedOperationException("Parameter must not be null");
    }
}
