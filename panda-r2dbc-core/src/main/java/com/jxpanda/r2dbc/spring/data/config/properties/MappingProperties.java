package com.jxpanda.r2dbc.spring.data.config.properties;

import com.jxpanda.r2dbc.spring.data.core.enhance.strategy.NamingStrategy;
import com.jxpanda.r2dbc.spring.data.core.enhance.strategy.ValidationStrategy;

/**
 * @param namingStrategy     字段命名策略，默认是不处理
 * @param validationStrategy 字段验证策略
 *                           优先级列表：字段注解（@TableColumn） > 类注解（@TableEntity） > 全局配置（R2dbcProperty）
 */
public record MappingProperties(NamingStrategy namingStrategy, ValidationStrategy validationStrategy) {

    public static MappingProperties empty() {
        return new MappingProperties(NamingStrategy.DEFAULT, ValidationStrategy.DEFAULT);
    }

}
