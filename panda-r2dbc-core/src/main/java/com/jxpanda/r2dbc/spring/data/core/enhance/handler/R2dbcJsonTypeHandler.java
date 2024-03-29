package com.jxpanda.r2dbc.spring.data.core.enhance.handler;

import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

/**
 * JSON处理器
 * 默认使用Jackson
 */
public abstract class R2dbcJsonTypeHandler<O, V> implements R2dbcTypeHandler<O, V> {

    protected abstract O readFromJson(byte[] jsonBytes, RelationalPersistentProperty property);

    protected abstract O readFromJson(V json, RelationalPersistentProperty property);

    protected abstract V writeToJson(O object);

    @Override
    public Writer<V, O> getWriter(RelationalPersistentProperty property) {
        return this::writeToJson;
    }

    @Override
    public Reader<O, V> getReader(RelationalPersistentProperty property) {
        return (v -> {
            if (v instanceof byte[] bytes) {
                return readFromJson(bytes, property);
            }
            return readFromJson(v, property);
        });
    }

}
