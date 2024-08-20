package com.jxpanda.r2dbc.spring.data.config;

import com.jxpanda.r2dbc.spring.data.config.properties.DatabaseProperties;
import com.jxpanda.r2dbc.spring.data.config.properties.LogicDeletePluginProperties;
import com.jxpanda.r2dbc.spring.data.config.properties.MappingProperties;
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

    public static DatabaseProperties getDatabaseProperties() {
        return propertiesOptional.map(R2dbcConfigProperties::database)
                .orElseThrow();
    }

    public static MappingProperties getMappingProperties() {
        return propertiesOptional.map(R2dbcConfigProperties::mapping)
                .orElseThrow();
    }

    public static LogicDeletePluginProperties getLogicDeleteProperties() {
        return propertiesOptional.map(it -> it.plugin().logicDelete())
                .orElseThrow();
    }

}
