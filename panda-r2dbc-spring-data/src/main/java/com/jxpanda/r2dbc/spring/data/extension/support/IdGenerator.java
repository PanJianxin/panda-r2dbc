package com.jxpanda.r2dbc.spring.data.extension.support;

import com.jxpanda.r2dbc.spring.data.extension.constant.StringConstant;

import java.io.Serializable;

/**
 * ID生成器接口
 */
public interface IdGenerator<T extends Serializable> {

    /**
     * 生成一个ID
     */
    T generate();

    /**
     * 返回ID是否有效
     * 默认判定是否为空，空则无效，不空则有效
     * 子类可以重写做其他判定规则
     */
    default boolean isIdEffective(T id) {
        return IdKit.isIdEffective(id);
    }

}
