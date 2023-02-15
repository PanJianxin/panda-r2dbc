package com.jxpanda.r2dbc.spring.data.config;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@AllArgsConstructor
public final class R2dbcEnvironment {

    private final R2dbcConfigProperties r2dbcConfigProperties;
    private static Optional<R2dbcConfigProperties> propertiesOptional = Optional.empty();

    @PostConstruct
    private void init() {
        propertiesOptional = Optional.of(r2dbcConfigProperties);
    }

    public static R2dbcConfigProperties.Database getDatabase() {
        return propertiesOptional.map(R2dbcConfigProperties::database)
                .orElseThrow();
    }

    public static R2dbcConfigProperties.Mapping getMapping() {
        return propertiesOptional.map(R2dbcConfigProperties::mapping)
                .orElseThrow();
    }

    public static R2dbcConfigProperties.LogicDelete getLogicDelete() {
        return propertiesOptional.map(R2dbcConfigProperties::logicDelete)
                .orElseThrow();
    }

}
