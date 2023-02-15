package com.jxpanda.r2dbc.spring.data.core.enhance.query;

import com.jxpanda.r2dbc.spring.data.core.enhance.query.criteria.EnhancedCriteria;
import lombok.Getter;
import org.springframework.data.domain.Sort;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.query.Query;

@Getter
@SuppressWarnings("UnusedReturnValue")
public class QueryWrapper {

    private final Query query;

    public QueryWrapper(Query query) {
        this.query = query;
    }

    private QueryWrapper(CriteriaDefinition criteriaDefinition) {
        this.query = Query.query(criteriaDefinition);
    }

    public static QueryWrapper query(CriteriaDefinition criteriaDefinition) {
        return new QueryWrapper(criteriaDefinition);
    }

    public QueryWrapper sort(Sort sort) {
        return new QueryWrapper(query.sort(sort));
    }

    public static void main(String[] args) {

        EnhancedCriteria enhancedCriteria = EnhancedCriteria.where("").is("");

        QueryWrapper.query(enhancedCriteria);

    }


}
