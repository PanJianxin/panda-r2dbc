package com.jxpanda.r2dbc.spring.data.core.enhance.strategy;

/**
 * 空值处理策略
 */
public enum NullStrategy {

    /**
     * 允许为空
     */
    NULLABLE,

    /**
     * 仅读数据的时候可以为null
     * 即：从数据库中读取数据映射到Entity的过程中，不处理null值
     * 此时，【写】数据的时候，会把null值用默认值替换掉
     */
    READ_NULL,

    /**
     * 仅写数据的时候可以为null
     * 即：把Entity写入到数据库的过程中，不处理null值
     * 此时，【读】数据的时候，会把null值用默认值替换掉
     */
    WRITE_NULL,

    /**
     * 不允许为空，会使用默认值填充、替换值为null的字段
     * 不论读、写，全都是用默认值替换null值
     */
    NONNULL,



}