package com.jxpanda.autoconfigure;

import com.jxpanda.r2dbc.spring.data.config.R2dbcConfigProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * 自定义配置文件，不与spring的冲突，可以兼容spring的配置
 *
 * @param mapping     映射相关配置
 * @param logicDelete 逻辑删除相关配置
 */
@ConfigurationProperties(prefix = "panda.r2dbc")
public record R2dbcProperties(
        @NestedConfigurationProperty
        R2dbcConfigProperties.Mapping mapping,
        @NestedConfigurationProperty
        R2dbcConfigProperties.LogicDelete logicDelete
) {

    public R2dbcConfigProperties transfer() {
        return new R2dbcConfigProperties(mapping(), logicDelete());
    }

}
