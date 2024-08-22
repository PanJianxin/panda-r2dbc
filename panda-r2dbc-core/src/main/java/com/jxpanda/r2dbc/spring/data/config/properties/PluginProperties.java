package com.jxpanda.r2dbc.spring.data.config.properties;


import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * @param logicDelete 逻辑删除插件配置
 */
public record PluginProperties(
        @NestedConfigurationProperty
        LogicDeletePluginProperties logicDelete
) {
}
