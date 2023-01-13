package com.jxpanda.autoconfigure;

import com.jxpanda.r2dbc.spring.data.config.R2dbcConfigProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "panda.r2dbc")
public record R2dbcProperties(
        @NestedConfigurationProperty
        R2dbcConfigProperties.Mapping mapping,
        @NestedConfigurationProperty
        R2dbcConfigProperties.LogicDelete logicDelete
) {


//
//    /**
//     * 逻辑删除配置
//     */
//    @NestedConfigurationProperty
//    private R2dbcMappingProperties.LogicDelete logicDelete;
//
//    /**
//     * 字段命名策略，默认是不处理
//     */
//    private NamingPolicy namingPolicy = NamingPolicy.DEFAULT;
//
//    /**
//     * 字段验证策略
//     * 优先级列表：字段注解（@TableColumn） > 类注解（@TableEntity） > 全局配置（R2dbcProperty）
//     */
//    private ValidationPolicy validationPolicy = ValidationPolicy.NOT_NULL;


    public R2dbcConfigProperties transfer() {
        return new R2dbcConfigProperties(mapping(), logicDelete());
    }

//    /**
//     * @param enable      是否开启逻辑删除
//     * @param field       逻辑删除的字段，优先级低于entity上的注解
//     * @param undeleteValue 逻辑删除「正常值」的标记
//     * @param deleteValue 逻辑删除「删除值」的标记
//     */
//    public record LogicDelete(
//            boolean enable, String field, String undeleteValue, String deleteValue
//    ) {
//
//        public R2dbcMappingProperties.LogicDelete transfer() {
//            return new R2dbcMappingProperties.LogicDelete(enable, field, undeleteValue, deleteValue);
//        }
//
//    }

}
