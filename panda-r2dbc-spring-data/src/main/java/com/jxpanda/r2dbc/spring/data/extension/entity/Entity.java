package com.jxpanda.r2dbc.spring.data.extension.entity;

import java.io.Serializable;

/**
 * entity接口
 */
public interface Entity<I> extends Serializable {

    String ID = "id";

    I getId();

    void setId(I id);

    default boolean isEffective() {
        return getId() != null;
    }

}
