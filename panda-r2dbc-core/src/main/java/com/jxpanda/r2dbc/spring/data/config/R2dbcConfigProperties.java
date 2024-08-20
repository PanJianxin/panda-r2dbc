package com.jxpanda.r2dbc.spring.data.config;

import com.jxpanda.r2dbc.spring.data.config.properties.DatabaseProperties;
import com.jxpanda.r2dbc.spring.data.config.properties.MappingProperties;
import com.jxpanda.r2dbc.spring.data.config.properties.PluginProperties;

/**
 * @param database 数据库相关配置
 * @param mapping  映射相关配置
 * @param plugin   插件相关配置
 */
public record R2dbcConfigProperties(
        DatabaseProperties database,
        MappingProperties mapping,
        PluginProperties plugin) {
}
