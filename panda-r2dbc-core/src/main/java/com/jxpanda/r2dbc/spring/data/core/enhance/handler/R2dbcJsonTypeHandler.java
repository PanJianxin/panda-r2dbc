package com.jxpanda.r2dbc.spring.data.core.enhance.handler;

import com.jxpanda.r2dbc.spring.data.infrastructure.kit.ReflectionKit;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

/**
 * JSON处理器
 * 默认使用Jackson
 */
public abstract class R2dbcJsonTypeHandler<O, V> implements R2dbcTypeHandler<O, V> {

    protected JsonValueConvert<V> jsonValueConvert;

    protected R2dbcJsonTypeHandler() {
        jsonValueConvert = new JsonValueConvert<>();
    }

    protected abstract O readFromJson(byte[] jsonBytes, RelationalPersistentProperty property);

    protected abstract O readFromJson(String json, RelationalPersistentProperty property);

    protected abstract String writeToJson(O object);


    @Override
    public Writer<V, O> getWriter(RelationalPersistentProperty property) {
        return (object -> this.jsonValueConvert.cast(writeToJson(object)));
    }

    @Override
    public Reader<O, V> getReader(RelationalPersistentProperty property) {
        return (v -> {
            if (v instanceof byte[] bytes) {
                return readFromJson(bytes, property);
            }
            return readFromJson(this.jsonValueConvert.toString(v), property);
        });
    }

    protected static class JsonValueConvert<V> {
        protected V cast(String json) {
            return ReflectionKit.cast(json);
        }

        protected String toString(V json) {
            return json.toString();
        }

    }


}
