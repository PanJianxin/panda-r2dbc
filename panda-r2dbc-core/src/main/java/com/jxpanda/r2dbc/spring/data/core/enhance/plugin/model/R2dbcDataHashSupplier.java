package com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model;

import reactor.core.publisher.Mono;

public interface R2dbcDataHashSupplier {

    <T> Mono<T> hash(T entity);

    <T> Mono<T> mergeHash(T oldEntity, T newEntity);

}
