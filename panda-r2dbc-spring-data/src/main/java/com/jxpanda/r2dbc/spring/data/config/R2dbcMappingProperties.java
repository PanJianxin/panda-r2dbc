package com.jxpanda.r2dbc.spring.data.config;

import com.jxpanda.r2dbc.spring.data.extension.policy.NamingPolicy;
import com.jxpanda.r2dbc.spring.data.extension.policy.ValidationPolicy;
import org.springframework.context.annotation.Configuration;

/**
 * @param forceQuote       是否在对象映射的过程中强制加上引用符
 *                         例如：在MySQL中，使用反引号'`'来做引用标识符，则注入SQL：SELECT XXX FROM order 会变为 SELECT XXX FROM `order`
 *                         使用此配置可以一定程度上避免SQL语句中出现关键字而导致的BAD SQL错误
 *                         默认是true
 * @param dataCenterId     数据中心ID，影响雪花算法ID生成规则，如果不使用雪花算法，这个配置不生效
 * @param workerId         工作节点ID，影响雪花算法ID生成规则，如果不使用雪花算法，这个配置不生效
 * @param namingPolicy     字段命名策略，默认是不处理
 * @param validationPolicy 字段验证策略
 *                         优先级列表：字段注解（@TableColumn） > 类注解（@TableEntity） > 全局配置（R2dbcProperty）
 */
public record R2dbcMappingProperties(boolean forceQuote,
                                     int dataCenterId, int workerId,
                                     LogicDelete logicDelete,
                                     NamingPolicy namingPolicy,
                                     ValidationPolicy validationPolicy) {


    /**
     * @param enable        是否开启逻辑删除
     * @param field         逻辑删除的字段，优先级低于entity上的注解
     * @param undeleteValue 逻辑删除「未删除值」的标记
     * @param deleteValue   逻辑删除「删除值」的标记
     */
    public record LogicDelete(
            boolean enable, String field, String undeleteValue, String deleteValue
    ) {
    }

}
