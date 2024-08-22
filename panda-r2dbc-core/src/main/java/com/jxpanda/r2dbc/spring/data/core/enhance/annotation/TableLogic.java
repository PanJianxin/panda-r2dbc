package com.jxpanda.r2dbc.spring.data.core.enhance.annotation;

import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.value.DefaultValueHandler;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.value.LogicDeleteValueType;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.value.PluginValueHandler;
import com.jxpanda.r2dbc.spring.data.infrastructure.constant.StringConstant;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

/**
 * 逻辑删除标记字段
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {FIELD})
public @interface TableLogic {

    /**
     * 是否开启逻辑删除
     * 字段上的配置优先级是最高的
     * 这个配置的意义在于，支持在全局配置打开\关闭逻辑删除的情况下啊
     * 独立控制当前对象的逻辑删除是否开启
     */
    boolean enable() default true;

    /**
     * 逻辑删除值的类型，枚举中已经配置了常用的类型，如果类型不支持，可以自定义类型
     * 默认是跟随全局配置
     */
    LogicDeleteValueType type() default LogicDeleteValueType.USE_PROPERTIES;


    /**
     * 被删除的值「数据已删除」
     * 默认逻辑删除值
     * 默认情况下，只要字段的值不等于「undeleteValue」则表示该字段「被删除」
     * 只有type == CUSTOMER的时候才生效
     */
    String deleteValue() default StringConstant.BLANK;

    /**
     * 删除值的类型处理器
     * 只有type == CUSTOMER的时候才生效
     */
    Class<? extends PluginValueHandler> deleteValueHandler() default DefaultValueHandler.class;

    /**
     * 正常情况下的值「数据未删除」
     * 默认逻辑未删除值
     * 默认情况下，只要字段的值等于「undeleteValue」则表示该字段「未被删除」
     * 只有type == CUSTOMER的时候才生效
     */
    String undeleteValue() default StringConstant.BLANK;

    /**
     * 未删除值的类型处理器
     * 只有type == CUSTOMER的时候才生效
     */
    Class<? extends PluginValueHandler> undeleteValueHandler() default DefaultValueHandler.class;

}
