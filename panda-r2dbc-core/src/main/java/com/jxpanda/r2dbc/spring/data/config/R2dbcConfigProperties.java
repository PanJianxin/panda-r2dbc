package com.jxpanda.r2dbc.spring.data.config;

import com.jxpanda.r2dbc.spring.data.config.properties.DatabaseProperties;
import com.jxpanda.r2dbc.spring.data.config.properties.MappingProperties;
import com.jxpanda.r2dbc.spring.data.config.properties.PluginProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * 自定义配置文件，不与spring的冲突，可以兼容spring的配置
 *
 * @param database 数据库相关配置
 * @param mapping  映射相关配置
 * @param plugin   插件相关配置
 */
@ConfigurationProperties(prefix = "panda.r2dbc")
public record R2dbcConfigProperties(
        @NestedConfigurationProperty
        DatabaseProperties database,
        @NestedConfigurationProperty
        MappingProperties mapping,
        @NestedConfigurationProperty
        PluginProperties plugin) {
}
