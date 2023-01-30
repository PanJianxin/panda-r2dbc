package com.jxpanda.r2dbc.spring.data.core.enhance.strategy;

import com.jxpanda.r2dbc.spring.data.core.enhance.key.IdGenerator;

/**
 * ID生成策略
 * 当对象的id字段没有传递的时候才生效
 */
public enum IdStrategy {

    /**
     * 默认的，优先级低于全局配置
     */
    DEFAULT,

    /**
     * 数据库自增，需要数据库配置自增
     */
    DATABASE_AUTO,

    /**
     * 使用generator，需要配置一个idGenerator的bean
     * 具体生成方式由生成器控制（默认是雪花算法）
     * {@link IdGenerator}
     */
    USE_GENERATOR

}
