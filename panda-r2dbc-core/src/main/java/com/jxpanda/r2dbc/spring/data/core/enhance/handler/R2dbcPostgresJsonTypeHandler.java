package com.jxpanda.r2dbc.spring.data.core.enhance.handler;

import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

public class R2dbcPostgresJsonTypeHandler<T> extends R2dbcJsonTypeHandler<T, Json> {
    @Override
    protected T readFromJson(byte[] jsonBytes, RelationalPersistentProperty property) {
        return null;
    }

    @Override
    protected T readFromJson(Json json, RelationalPersistentProperty property) {
        return null;
    }

    @Override
    protected Json writeToJson(T object) {
        return null;
    }

    @Override
    public Json write(T object, RelationalPersistentProperty property) {
        return super.write(object, property);
    }

    @Override
    public T read(Json value, RelationalPersistentProperty property) {
        return super.read(value, property);
    }
}
