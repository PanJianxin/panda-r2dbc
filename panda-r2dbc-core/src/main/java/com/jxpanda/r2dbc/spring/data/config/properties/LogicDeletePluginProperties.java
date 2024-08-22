package com.jxpanda.r2dbc.spring.data.config.properties;

import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.value.LogicDeleteValue;
import com.jxpanda.r2dbc.spring.data.infrastructure.constant.StringConstant;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * @param enable 是否开启逻辑删除
 * @param field  逻辑删除的字段，优先级低于entity上的注解
 * @param value  值对象
 */
public record LogicDeletePluginProperties(boolean enable,
                                          String field,
                                          @NestedConfigurationProperty
                                          LogicDeleteValue value) {
    public static LogicDeletePluginProperties empty() {
        return new LogicDeletePluginProperties(false, StringConstant.BLANK, LogicDeleteValue.empty());
    }

}