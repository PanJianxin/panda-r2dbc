package com.jxpanda.r2dbc.spring.data.core.enhance.annotation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.Reference;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Criteria;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.BiFunction;

import static java.lang.annotation.ElementType.FIELD;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {FIELD})
@Reference
@TableColumn(exists = false, isJson = true)
public @interface TableReference {

    KeyType keyType() default KeyType.ID;

    String keyColumn() default "";

    String referenceColumn();

    ReferenceCondition referenceCondition() default ReferenceCondition.EQ;


    @Getter
    @AllArgsConstructor
    enum KeyType {
        ID((entity, keyColumn) -> entity.getIdProperty()),
        COLUMN(PersistentEntity::getPersistentProperty);
        private final BiFunction<RelationalPersistentEntity<?>, String, RelationalPersistentProperty> referenceProperty;
    }

    @Getter
    @AllArgsConstructor
    enum ReferenceCondition {
        EQ(Criteria.CriteriaStep::is),
        IN(Criteria.CriteriaStep::in);
        private final BiFunction<Criteria.CriteriaStep, Object, Criteria> condition;
    }

}
