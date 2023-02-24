package com.jxpanda.r2dbc.spring.data.core.kit;

import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.util.Assert;

import java.util.Collection;

public class QueryKit {

    public static <T> CriteriaAdapter<T> with(Class<T> clazz) {
        return new CriteriaAdapter<>(clazz);
    }

    public static <T, ID> Query queryById(Class<T> clazz, ID id) {
        return Query.query(with(clazz).whereId().is(id));
    }

    public static <T, ID> Query queryById(Class<T> clazz, Collection<ID> ids) {
        return Query.query(with(clazz).whereId().is(ids));
    }

    public static class CriteriaAdapter<T> {

        private final Class<T> clazz;

        public CriteriaAdapter(Class<T> clazz) {
            this.clazz = clazz;
        }

        public Criteria.CriteriaStep whereId() {
            RelationalPersistentEntity<T> requiredEntity = R2dbcMappingKit.getRequiredEntity(this.clazz);
            Assert.isTrue(requiredEntity.hasIdProperty(), "Domain type must has id property");
            return Criteria.where(requiredEntity.getIdColumn().getReference());
        }


    }


}
