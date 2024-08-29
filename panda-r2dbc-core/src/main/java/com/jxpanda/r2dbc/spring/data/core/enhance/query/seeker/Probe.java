package com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker;

import com.jxpanda.r2dbc.spring.data.core.enhance.query.criteria.EnhancedCriteria;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker.domain.Extend;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker.domain.Rule;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker.domain.Synapse;
import com.jxpanda.r2dbc.spring.data.infrastructure.constant.StringConstant;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.StringKit;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.springframework.data.relational.core.query.Criteria;

@Data
@Builder
@NoArgsConstructor
public class Probe {
    /**
     * 字段名
     */
    @With
    @Builder.Default
    private String field = StringConstant.BLANK;
    /**
     * json类型字段的key（仅json类型需要使用）
     */
    @Builder.Default
    private String jsonKey = StringConstant.BLANK;
    /**
     * 只有值是允许为空的
     */
    private Object value;
    /**
     * 查询规则，默认是EQ(=)
     */
    @Builder.Default
    private Rule rule = Rule.EQ;
    /**
     * 扩展字段
     * 这个字段比较灵活
     * 可以用来处理一切预处理逻辑
     * 也可以预留以后需要扩展逻辑的时候使用
     */
    @Builder.Default
    private Extend extend = Extend.NONE;
    /**
     * 突触，探机与探机之间的链接逻辑
     * AND和OR两个取值
     * 默认是AND（与）逻辑
     */
    @Builder.Default
    private Synapse synapse = Synapse.AND;

    /**
     * 是否自动把字段转为snakeCase
     * 默认是true，自动转换把filed进行一次snakeCase的转换
     */
    @Builder.Default
    private boolean snakeCase = true;

    /**
     * 手动写一下全参数构造器
     * 对所有字段做空安全处理
     * 反编译了lombok的代码后发现
     * 如果要严格的管理空安全的问题
     * 提供字段的默认值还有一个原因，lombok的无参构造器会自动把默认值设置好
     * 需要提供值的默认值（对于builder是必须的）
     * 然后就是提供全参数构造器，在全参数构造器中处理默认值
     * 这样不论是builder模式还是with函数，对象的值属性都能保证是空安全的
     * 因为build()函数和with函数都最终调用了全参数构造器
     */
    public Probe(String field, String jsonKey, Object value, Rule rule, Extend extend, Synapse synapse, boolean snakeCase) {
        this.field = field == null ? StringConstant.BLANK : field;
        this.jsonKey = jsonKey == null ? StringConstant.BLANK : jsonKey;
        // 只有value允许为空
        this.value = value;
        this.rule = rule == null ? Rule.EQ : rule;
        this.extend = extend == null ? Extend.NONE : extend;
        this.synapse = synapse == null ? Synapse.AND : synapse;
        this.snakeCase = snakeCase;
    }

    public String getField() {
        // TODO: 这里需要接入NamingStrategy才合适，时间关系，这里的改动缓一下
        return snakeCase ? StringKit.snakeCase(this.field) : this.field;
    }

    public EnhancedCriteria apply(EnhancedCriteria criteria) {
        EnhancedCriteria.EnhancedCriteriaStep criteriaStep;
        if (criteria == null || criteria.isEmpty()) {
            criteriaStep = EnhancedCriteria.where(this.getField());
        } else {
            criteriaStep = getSynapse().execute(criteria, this.getField());
        }
        return getRule().execute(criteriaStep, this.getValue());
    }

}
