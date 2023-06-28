package com.jxpanda.r2dbc.spring.data.core.operation;

import org.springframework.data.r2dbc.core.ReactiveUpdateOperation;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface R2dbcUpdateOperation {

    <T> R2dbcUpdate<T> update(@NonNull Class<T> domainType);

    interface R2dbcUpdate<T> extends ReactiveUpdateOperation.ReactiveUpdate {

        Mono<T> using(T entity);

        Flux<T> batch(Collection<T> objectList);

    }

}
