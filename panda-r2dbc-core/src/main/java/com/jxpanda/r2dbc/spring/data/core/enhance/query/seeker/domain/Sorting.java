package com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Sort;

import java.util.function.Function;

@Getter
@AllArgsConstructor
public enum Sorting {
    /**
     * 正序排序
     */
    ASC(Sort.Order::asc),
    /**
     * 正序排序
     */
    ASCEND(Sort.Order::asc),
    /**
     * 正序排序
     */
    ASCENDING(Sort.Order::asc),
    /**
     * 倒序排序
     */
    DESC(Sort.Order::desc),
    /**
     * 倒序排序
     */
    DESCEND(Sort.Order::desc),
    /**
     * 倒序排序
     */
    DESCENDING(Sort.Order::desc);

    private final Function<String, Sort.Order> sortFunction;

    public Sort.Order execute(String field) {
        return sortFunction.apply(field);
    }

}