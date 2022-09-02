package com.jxpanda.r2dbc.spring.data.extension.policy;

/**
 * 命名策略
 */
public enum NamingPolicy {

    /**
     * 默认不做任何处理
     */
    NONE,
    /**
     * 下划线分隔命名
     */
    SNAKE_CASE,
    /**
     * 驼峰命名
     */
    CAMEL_CASE

}
