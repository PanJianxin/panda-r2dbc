package com.jxpanda.r2dbc.spring.data.extension.entity;

import java.io.Serializable;

/**
 * entity接口
 */
public interface Entity<I> extends Serializable {

    String ID = "id";

    I getId();

    void setId(I id);

    /**
     * 返回当前entity是否有效
     * 默认是判断id是否为null
     */
    default boolean isEffective() {
        return getId() != null;
    }

    default boolean isNotEffective() {
        return !isEffective();
    }

}
