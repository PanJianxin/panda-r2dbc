package com.jxpanda.autoconfigure;

import com.jxpanda.r2dbc.spring.data.config.R2dbcMappingProperties;
import com.jxpanda.r2dbc.spring.data.extension.policy.NamingPolicy;
import com.jxpanda.r2dbc.spring.data.extension.policy.ValidationPolicy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "panda.r2dbc")
public class R2dbcProperties {

    /**
     * 是否在对象映射的过程中强制加上引用符
     * 例如：在MySQL中，使用反引号'`'来做引用标识符，则注入SQL：SELECT XXX FROM order 会变为 SELECT XXX FROM `order`
     * 使用此配置可以一定程度上避免SQL语句中出现关键字而导致的BAD SQL错误
     * 默认是true
     */
    private boolean forceQuote = true;

    /**
     * 数据中心ID，影响雪花算法ID生成规则，如果不使用雪花算法，这个配置不生效
     */
    private int dataCenterId = 0;

    /**
     * 工作节点ID，影响雪花算法ID生成规则，如果不使用雪花算法，这个配置不生效
     */
    private int workerId = 0;

    /**
     * 逻辑删除配置
     */
    private R2dbcMappingProperties.LogicDelete logicDelete;

    /**
     * 字段命名策略，默认是不处理
     */
    private NamingPolicy namingPolicy = NamingPolicy.DEFAULT;

    /**
     * 字段验证策略
     * 优先级列表：字段注解（@TableColumn） > 类注解（@TableEntity） > 全局配置（R2dbcProperty）
     */
    private ValidationPolicy validationPolicy = ValidationPolicy.NOT_NULL;


    public R2dbcMappingProperties transfer() {
        return new R2dbcMappingProperties(forceQuote, dataCenterId, workerId, logicDelete, namingPolicy, validationPolicy);
    }


}
