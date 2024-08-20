package com.jxpanda.r2dbc.spring.data.config.properties;

import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2DbcLogicDeletePlugin;
import com.jxpanda.r2dbc.spring.data.infrastructure.constant.StringConstant;

/**
 * @param enable        是否开启逻辑删除
 * @param field         逻辑删除的字段，优先级低于entity上的注解
 * @param undeleteValue 逻辑删除「未删除值」的标记
 * @param deleteValue   逻辑删除「删除值」的标记
 */
public record LogicDeletePluginProperties(boolean enable,
                                          String field,
                                          R2DbcLogicDeletePlugin.Value undeleteValue,
                                          R2DbcLogicDeletePlugin.Value deleteValue) {
    public static LogicDeletePluginProperties empty() {
        return new LogicDeletePluginProperties(false, StringConstant.BLANK, R2DbcLogicDeletePlugin.Value.empty(), R2DbcLogicDeletePlugin.Value.empty());
    }

}