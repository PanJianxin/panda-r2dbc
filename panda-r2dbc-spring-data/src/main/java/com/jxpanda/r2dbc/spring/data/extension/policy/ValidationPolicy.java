package com.jxpanda.r2dbc.spring.data.extension.policy;

import lombok.AllArgsConstructor;
import org.springframework.util.ObjectUtils;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * 字段验证策略
 */
@AllArgsConstructor
public enum ValidationPolicy {

    /**
     * 默认不错任何判断
     * 所有情况都视为有效
     */
    NOT_CHECK(it -> true),

    NOT_NULL(Objects::nonNull),

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

//    public void apply(BiConsumer<OutboundRow, RelationalPersistentProperty> consumer) {
//        consumer.accept();
//    }

}
