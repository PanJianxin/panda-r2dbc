package com.jxpanda.r2dbc.spring.data.core.enhance.annotation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.Reference;
import org.springframework.data.relational.core.query.Criteria;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiFunction;

import static java.lang.annotation.ElementType.FIELD;

/**
 * @author Panda
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {FIELD})
@Reference
@TableColumn(exists = false, isJson = true)
public @interface TableReference {

    /**
     * 主表字段
     */
    String keyColumn() default "";

    /**
     * 分隔符
     */
    String delimiter() default "";

    /**
     * 关联表的字段
     */
    String referenceColumn();

    /**
     * 关联使用的条件，目前支持EQ和IN两种
     */
    ReferenceCondition referenceCondition() default ReferenceCondition.EQ;


    @Getter
    @AllArgsConstructor
    enum ReferenceCondition {
        /**
         * eq
         */
        EQ(Criteria.CriteriaStep::is),
        /**
         * in
         */
        IN(((criteriaStep, value) -> {
            Collection<?> inValue = new ArrayList<>();
            if (value instanceof Object[] arrayValue) {
                inValue = Arrays.stream(arrayValue).toList();
            } else if (value instanceof Collection<?> collectionValue){
                inValue = collectionValue;
            }
            return criteriaStep.in(inValue);
        }));
        private final BiFunction<Criteria.CriteriaStep, Object, Criteria> condition;
    }

}
