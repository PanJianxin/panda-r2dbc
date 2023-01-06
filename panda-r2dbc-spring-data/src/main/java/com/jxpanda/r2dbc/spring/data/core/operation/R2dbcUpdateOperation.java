package com.jxpanda.r2dbc.spring.data.core.operation;

import org.springframework.data.r2dbc.core.ReactiveUpdateOperation;
import reactor.core.publisher.Mono;

public interface R2dbcUpdateOperation extends ReactiveUpdateOperation {

    interface R2dbcUpdate extends ReactiveUpdate {

        <T> Mono<T> using(T entity);

    }

}
