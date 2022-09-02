package com.jxpanda.r2dbc.spring.data.extension.handler;

import com.jxpanda.r2dbc.spring.data.extension.annotation.TableColumn;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

public interface R2dbcTypeHandler<O, V> {

    Writer<V, O> getWriter(RelationalPersistentProperty property);

    Reader<O, V> getReader(RelationalPersistentProperty property);

    /**
     * 把对象序列化为值
     * 即：
     * value -> object
     *
     * @param property 对象的类型
     * @param object   对象
     * @return 序列化之后的值
     */
    default V write(O object, RelationalPersistentProperty property) {
        return getWriter(property).apply(object);
    }

    /**
     * 把值反序列化为对象
     * 即：
     * value -> object
     *
     * @param property 对象的类型
     * @param value    值
     * @return 反序列化之后的对象
     */
    default O read(V value, RelationalPersistentProperty property) {
        return getReader(property).apply(value);
    }

    /**
     * 什么都不处理的类型处理器
     * 是一个标记对象
     * 用于在 {@link TableColumn} 注解中作为默认的类型处理器
     */
    class DefaultHandler implements R2dbcTypeHandler<Object, Object> {

        @Override
        public Writer<Object, Object> getWriter(RelationalPersistentProperty property) {
            return (object) -> object;
        }

        @Override
        public Reader<Object, Object> getReader(RelationalPersistentProperty property) {
            return (value) -> value;
        }
    }

    /**
     * 强制忽略处理器
     * 是一个标记对象
     * 用于在 {@link TableColumn} 注解中标识
     * 被这个类型标识的字段，会强制忽略处理器的处理（无论什么类型）
     * 与{@link DefaultHandler} 的区别是，DefaultHandler的优先级是低于IgnoreHandler的
     * DefaultHandler如果标识了「枚举」「非基本类型」的字段
     * 会尝试基于类型使用注册的handler对值进行一次处理
     * 如果主动指定了IgnoreHandler则忽略所有处理器
     */
    class IgnoreHandler implements R2dbcTypeHandler<Object, Object> {

        @Override
        public Writer<Object, Object> getWriter(RelationalPersistentProperty property) {
            return (object) -> object;
        }

        @Override
        public Reader<Object, Object> getReader(RelationalPersistentProperty property) {
            return (value) -> value;
        }
    }

    @FunctionalInterface
    interface Writer<V, O> {
        /**
         * 把对象序列化为值
         * 即：
         * object -> value
         *
         * @param object 对象
         * @return 序列化之后的值
         */
        V apply(O object);
    }

    @FunctionalInterface
    interface Reader<O, V> {
        /**
         * 把值反序列化为对象
         * 即：
         * value -> object
         *
         * @param value 值
         * @return 反序列化之后的对象
         */
        O apply(V value);
    }

}
