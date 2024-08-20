package com.jxpanda.r2dbc.spring.data.config.properties;

import com.jxpanda.r2dbc.spring.data.core.enhance.strategy.IdStrategy;

/**
 * @param forceQuote   是否在对象映射的过程中强制加上引用符
 *                     例如：在MySQL中，使用反引号'`'来做引用标识符，则注入SQL：SELECT XXX FROM order 会变为 SELECT XXX FROM `order`
 *                     使用此配置可以一定程度上避免SQL语句中出现关键字而导致的BAD SQL错误
 *                     默认是true
 * @param idStrategy   ID生成策略 默认是MANUAL，即手动处理
 * @param dataCenterId 数据中心ID，影响雪花算法ID生成规则，如果不使用雪花算法，这个配置不生效
 * @param workerId     工作节点ID，影响雪花算法ID生成规则，如果不使用雪花算法，这个配置不生效
 */
public record DatabaseProperties(boolean forceQuote, IdStrategy idStrategy, int dataCenterId, int workerId) {

    public static DatabaseProperties empty() {
        return new DatabaseProperties(false, IdStrategy.DEFAULT, 0, 0);
    }


}
