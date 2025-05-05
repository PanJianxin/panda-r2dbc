package com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker.domain;

import com.jxpanda.r2dbc.spring.data.core.enhance.query.criteria.EnhancedCriteria;
import org.springframework.data.relational.core.query.Criteria;

import java.util.Collection;
import java.util.function.BiFunction;

public interface RuleFunction {

    BiFunction<Criteria.CriteriaStep, Object, Criteria> getFunction();

    default boolean allowNull(){
        return false;
    }

    default Criteria execute(Criteria.CriteriaStep criteriaStep, Object param) {
        if (!allowNull() && param == null) {
            return nullParamFallback(criteriaStep);
        }
        return getFunction().apply(criteriaStep, param);
    }

    default Criteria nullParamFallback(Criteria.CriteriaStep criteriaStep) {
        throw new UnsupportedOperationException("Parameter must not be null");
    }
}
