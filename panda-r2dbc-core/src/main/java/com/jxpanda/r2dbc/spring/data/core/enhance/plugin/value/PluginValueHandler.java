package com.jxpanda.r2dbc.spring.data.core.enhance.plugin.value;

import org.springframework.lang.NonNull;

/**
 * 插件值配置处理器
 * 作用是把配置的值转换成插件需要的值
 * 由于配置文件配置的属性值都是字符串，所以这里需要把配置的值转换成插件需要的值
 */
public interface PluginValueHandler {

    @NonNull
    default Object covert(@NonNull String value) {
        return value;
    }

}
