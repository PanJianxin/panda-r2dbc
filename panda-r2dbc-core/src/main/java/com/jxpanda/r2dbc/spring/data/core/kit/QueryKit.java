package com.jxpanda.r2dbc.spring.data.core.kit;

import com.jxpanda.r2dbc.spring.data.core.enhance.query.criteria.EnhancedCriteria;
import lombok.experimental.UtilityClass;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * @author Panda
 */
@UtilityClass
public class QueryKit {
    public static <T, ID> Query queryById(Class<T> clazz, ID id) {
        return Query.query(whereId(clazz).is(id));
    }

    public static <T, ID> Query queryByIds(Class<T> clazz, Collection<ID> ids) {
        return Query.query(whereId(clazz).in(ids));
    }

    public static Mono<CriteriaDefinition> criteriaMono(CriteriaDefinition criteria) {
        return Mono.just(criteria == null ? Criteria.empty() : criteria);
    }


    public static CriteriaDefinition mergeCriteria(@Nullable CriteriaDefinition original, @Nullable CriteriaDefinition target) {
        if (original == null || target == null) {
            return original != null ? original : target;
        }
        CriteriaDefinition mergedCriteria = original;
        if (original instanceof Criteria criteriaOriginal) {
            CriteriaDefinition.Combinator combinator = target.getCombinator();
            if (combinator == CriteriaDefinition.Combinator.OR) {
                mergedCriteria = criteriaOriginal.or(target);
            } else {
                mergedCriteria = criteriaOriginal.and(target);
            }
        }
        if (original instanceof EnhancedCriteria enhancedCriteriaOriginal) {
            CriteriaDefinition.Combinator combinator = target.getCombinator();
            if (combinator == CriteriaDefinition.Combinator.OR) {
                mergedCriteria = enhancedCriteriaOriginal.or(target);
            } else {
                mergedCriteria = enhancedCriteriaOriginal.and(target);
            }
        }
        return mergedCriteria;
    }

    public static Update meergeUpdate(Update original, Update target){
        if (original == null || target == null) {
            return original != null ? original : target;
        }
        original.getAssignments().forEach(target::set);
        return target;
    }

    private static <T> Criteria.CriteriaStep whereId(Class<T> clazz) {
        RelationalPersistentEntity<T> requiredEntity = R2dbcMappingKit.getRequiredEntity(clazz);
        return Criteria.where(requiredEntity.getIdColumn().getReference());
    }

}
