package com.jxpanda.r2dbc.spring.data.mapping.handler;

public abstract class TypeHandler<O, V> {


    public abstract Serialize<O, V> getSerializer(Class<? extends O> objectClass);

    public abstract Deserializer<O, V> getDeserializer(Class<? extends O> objectClass);

    /**
     * 把对象序列化为值
     * 即：
     * value -> object
     *
     * @param objectClass 对象的类型
     * @param object      对象
     * @return 序列化之后的值
     */
    public V serialize(Class<? extends O> objectClass, O object) {
        return getSerializer(objectClass).apply(object);
    }

    /**
     * 把值反序列化为对象
     * 即：
     * value -> object
     *
     * @param objectClass 对象的类型
     * @param value       值
     * @return 反序列化之后的对象
     */
    public O deserialize(Class<? extends O> objectClass, V value) {
        return getDeserializer(objectClass).apply(value);
    }


    @FunctionalInterface
    public interface Serialize<O, V> {
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
    public interface Deserializer<O, V> {
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
