package com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker.domain;

import com.jxpanda.r2dbc.spring.data.core.enhance.query.criteria.EnhancedCriteria;
import lombok.AllArgsConstructor;

import java.util.function.BiFunction;

@AllArgsConstructor
public enum Synapse {
    /**
     * 拼接AND条件
     */
    AND(EnhancedCriteria::and),
    /**
     * 拼接OR条件
     */
    OR(EnhancedCriteria::or);

    private final BiFunction<EnhancedCriteria, String, EnhancedCriteria.EnhancedCriteriaStep> synapseFunction;

    public EnhancedCriteria.EnhancedCriteriaStep execute(EnhancedCriteria criteria, String field) {
        return synapseFunction.apply(criteria, field);
    }

}