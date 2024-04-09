package com.jxpanda.r2dbc.spring.data.extension.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

/**
 * entity接口
 *
 * @author Panda
 */
public interface Entity<ID> extends Serializable {

    /**
     * 定义实体的ID字段，默认值为"id"
     */
    String ID = "id";

    /**
     * 获取实体的ID
     *
     * @return id
     */
    ID getId();

    /**
     * 返回当前entity是否有效
     * 默认是判断id是否为null
     *
     * @return boolean 返回true如果实体有效，否则返回false。实体有效的默认条件是id不为null。
     */
    @JsonIgnore
    default boolean isEffective() {
        // 判断id是否非空以确定实体是否有效
        return getId() != null;
    }

}
