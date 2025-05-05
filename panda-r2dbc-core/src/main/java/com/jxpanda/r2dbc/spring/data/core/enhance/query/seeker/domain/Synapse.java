package com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker.domain;

import com.jxpanda.r2dbc.spring.data.core.enhance.query.criteria.EnhancedCriteria;
import lombok.AllArgsConstructor;
import org.springframework.data.relational.core.query.Criteria;

import java.util.function.BiFunction;

@AllArgsConstructor
public enum Synapse {
    /**
     * 拼接AND条件
     */
    AND(Criteria::and),
    /**
     * 拼接OR条件
     */
    OR(Criteria::or);

    private final BiFunction<Criteria, String, Criteria.CriteriaStep> synapseFunction;

    public Criteria.CriteriaStep execute(Criteria criteria, String field) {
        return synapseFunction.apply(criteria, field);
    }

}