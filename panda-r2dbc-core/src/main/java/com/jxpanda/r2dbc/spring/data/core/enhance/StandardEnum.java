package com.jxpanda.r2dbc.spring.data.core.enhance;

/**
 * 标准枚举接口定义了枚举的基本行为。
 * 该接口要求枚举类型提供一个代码和描述。
 *
 * @author Panda*/
public interface StandardEnum {

    /**
     * 获取枚举项的代码。
     * @return 返回该枚举项的代码，类型为Integer。
     */
    Integer getCode();

    /**
     * 获取枚举项的描述。
     * @return 返回该枚举项的描述，类型为String。
     */
    String getDescription();
}

