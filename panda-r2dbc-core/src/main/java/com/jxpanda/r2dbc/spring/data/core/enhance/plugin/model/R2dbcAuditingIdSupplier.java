package com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model;

import reactor.core.publisher.Mono;

public interface R2dbcAuditingIdSupplier {

    Mono<String> getCreatorId();

    Mono<String> getUpdaterId();

}
