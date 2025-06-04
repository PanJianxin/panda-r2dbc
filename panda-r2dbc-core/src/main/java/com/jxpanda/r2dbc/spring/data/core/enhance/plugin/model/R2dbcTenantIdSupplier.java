package com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model;

import reactor.core.publisher.Mono;

@FunctionalInterface
public interface R2dbcTenantIdSupplier {

    Mono<String> get();

}
