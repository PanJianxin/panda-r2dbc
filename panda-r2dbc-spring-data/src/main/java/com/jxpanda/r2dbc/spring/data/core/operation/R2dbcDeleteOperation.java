package com.jxpanda.r2dbc.spring.data.core.operation;

import org.springframework.data.r2dbc.core.ReactiveDeleteOperation;
import reactor.core.publisher.Mono;

public interface R2dbcDeleteOperation extends ReactiveDeleteOperation {

    interface R2dbcDelete extends ReactiveDeleteOperation.ReactiveDelete {

        <T> Mono<Boolean> using(T entity);

    }

}
