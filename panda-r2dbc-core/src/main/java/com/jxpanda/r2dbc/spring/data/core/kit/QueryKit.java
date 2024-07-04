package com.jxpanda.r2dbc.spring.data.core.kit;

import lombok.experimental.UtilityClass;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;

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

    private static <T> Criteria.CriteriaStep whereId(Class<T> clazz) {
        RelationalPersistentEntity<T> requiredEntity = R2dbcMappingKit.getRequiredEntity(clazz);
        return Criteria.where(requiredEntity.getIdColumn().getReference());
    }

}
