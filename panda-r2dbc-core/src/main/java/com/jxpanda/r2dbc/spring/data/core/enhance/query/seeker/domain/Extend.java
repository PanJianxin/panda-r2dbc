package com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker.domain;

import com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker.Probe;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.DateTimeKit;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.EnumKit;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.ReflectionKit;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.StringKit;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 探机对象的扩展函数
 * 设计思路是让外界可以一定程度上干涉探机对象的处理逻辑
 * 干涉范围要求内部可以控制，所以用枚举来实现
 */
@SuppressWarnings("unchecked")
public enum Extend {

    /**
     * 不做处理
     */
    NONE,
    /**
     * 忽略，跳过该探机的条件逻辑
     */
    SKIP,
    /**
     * 禁止后端修改这个数据
     */
    DO_NOT_OVERRIDE,
    /**
     * 日期类型处理
     * 以下处理逻辑是与前端的约定
     * 如果是EQ逻辑，把逻辑强行转为BETWEEN，把入参拆成两个，入参时间的00:00:00和入参时间的23:59:59
     * 如果是BETWEEN逻辑，第一个参数设置时间到日期的00:00:00，第二个则设定到23:59:59
     * 如果是GT/GE逻辑，把入参时间调到00:00:00
     * 如果是LT/LE逻辑，把入参时间调到23:59:59
     */
    DATE {
        /**
         * 日期类型要重写预处理函数
         * */
        @Override
        public <T> Probe handle(Probe probe, Class<T> clazz) {

            // 与前端约定，所有的日期入参都以字符串的形式入参
            // 其格式为【yyyy-MM-dd】或【yyyy-MM-dd HH:mm:ss】
            switch (probe.getRule()) {
                case EQ:
                    probe.setRule(Rule.BETWEEN);
                    probe.setValue(DateTimeKit.alignToBeginAndEnd((String) probe.getValue(), true));
                    break;
                case BETWEEN:
                    var params = (List<String>) probe.getValue();
                    // 处理逻辑是这样的，如果一个时间是yyyy-MM-dd HH:mm:ss的格式，那就不做处理，以前端入参为准
                    // 否则，如果是第一个参数，日期设定到当天的00:00:00，第二个参数设定为23:59:59
                    probe.setValue(DateTimeKit.alignToBeginAndEnd(params.get(0), false, params.get(1), true));
                    break;
                case GT:
                case GE:
                    // 如果是GT/GE逻辑，把入参时间调到00:00:00
                    // 不强制覆盖，前端传递了时间的话，以前端为准
                    probe.setValue(DateTimeKit.alignToBegin((String) probe.getValue(), false));
                    break;
                case LT:
                case LE:
                    // 如果是LT/LE逻辑，把入参时间调到23:59:59
                    probe.setValue(DateTimeKit.alignToEnd((String) probe.getValue(), true));
                    break;
                default:
                    break;
            }
            return probe;
        }
    },
    /**
     * 枚举值的标记，如果是枚举值，要把值转换一次
     * 约定，前端会传递字符串类型进来，后端转换成枚举值对应的数字value
     */
    ENUM {
        @Override
        public <T> Probe handle(Probe probe, Class<T> clazz) {
            // 获取字段
            Class<?> enumType = ReflectionKit.getFieldMap(clazz).get(StringKit.camelCase(probe.getField())).getType();
            // 枚举的查询一般只有EQ、IN、NE、NOT_IN几种情况，所以就只处理这几种情况了，有其他的以后再加
            Rule rule = probe.getRule();
            if (rule == Rule.EQ || rule == Rule.NE) {
                probe.setValue(EnumKit.translate2Code((String) probe.getValue(), enumType));
            }
            if (rule == Rule.IN || rule == Rule.NOT_IN) {
                List<Integer> values = ((ArrayList<String>) probe.getValue()).stream().map(value -> EnumKit.translate2Code(value, enumType)).collect(Collectors.toList());
                probe.setValue(values);
            }
            return probe;
        }
    };

    /**
     * 探机对象的处理函数
     * 默认情况下是不做处理的
     */
    public <T> Probe handle(Probe probe, Class<T> clazz) {
        return probe;
    }

}
