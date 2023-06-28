package com.jxpanda.r2dbc.spring.data.core.enhance.strategy;

import lombok.AllArgsConstructor;
import org.springframework.util.ObjectUtils;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * 字段验证策略
 */
@AllArgsConstructor
public enum ValidationStrategy {

    /**
     * 默认值，不做任何判断
     * DEFAULT与NOT_CHECK的区别是
     * DEFAULT是作为默认值写在注解里的，这个值的优先级会小于全局配置文件的优先级
     * 也就是说，如果全局配置文件配置了策略，则以全局配置的为准
     */
    DEFAULT(it -> true),

    /**
     * 不做任何判断
     * 优先级会大于全局配置文件
     */
    NOT_CHECK(it -> true),

    /**
     * 不为null
     */
    NOT_NULL(Objects::nonNull),

    /**
     * 不为null且内容不是空的
     */
    NOT_EMPTY(it -> !ObjectUtils.isEmpty(it));

    /**
     * 断言函数
     */
    private final Predicate<Object> predicate;


    /**
     * 返回是否有效
     */
    public boolean isEffective(Object object) {
        return this.predicate.test(object);
    }

    public boolean isNotEffective(Object object) {
        return !isEffective(object);
    }

}
