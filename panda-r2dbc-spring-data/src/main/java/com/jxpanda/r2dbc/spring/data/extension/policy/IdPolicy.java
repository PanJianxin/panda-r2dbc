package com.jxpanda.r2dbc.spring.data.extension.policy;

/**
 * ID生成策略
 */
public enum IdPolicy {

    /**
     * 数据库自增，需要数据库自己配置
     * */
    DATABASE_AUTO,

    /**
     * 手动处理
     * */
    NONE

}
