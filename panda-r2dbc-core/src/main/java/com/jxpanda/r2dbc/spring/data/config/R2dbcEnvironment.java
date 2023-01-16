package com.jxpanda.r2dbc.spring.data.config;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
public class R2dbcEnvironment implements EnvironmentAware {

    private static final String MAPPING_KEY = "panda.r2dbc.mapping";
    private static final String LOGIC_DELETE_KEY = "panda.r2dbc.logic-delete";

    private static Optional<Environment> environmentOptional = Optional.empty();

    public static R2dbcConfigProperties.Mapping getMapping() {
        return environmentOptional.map(environment -> environment.getProperty(MAPPING_KEY, R2dbcConfigProperties.Mapping.class))
                .orElse(R2dbcConfigProperties.Mapping.empty());
    }


    public static R2dbcConfigProperties.LogicDelete getLogicDelete() {
        return environmentOptional.map(environment -> environment.getProperty(LOGIC_DELETE_KEY, R2dbcConfigProperties.LogicDelete.class))
                .orElse(R2dbcConfigProperties.LogicDelete.empty());
    }

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        R2dbcEnvironment.environmentOptional = Optional.of(environment);
    }
}
