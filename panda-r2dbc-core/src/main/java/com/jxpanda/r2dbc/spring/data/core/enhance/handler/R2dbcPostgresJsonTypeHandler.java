package com.jxpanda.r2dbc.spring.data.core.enhance.handler;

import io.r2dbc.postgresql.codec.Json;

public class R2dbcPostgresJsonTypeHandler<O> extends R2dbcJacksonTypeHandler<O, Json> {

    public R2dbcPostgresJsonTypeHandler() {
        jsonValueConvert = new JsonValueConvert<>() {
            @Override
            public Json cast(String json) {
                return Json.of(json);
            }

            @Override
            protected String toString(Json json) {
                return json.asString();
            }
        };
    }
}
