package com.jxpanda.r2dbc.spring.data.core.enhance.plugin;

import reactor.core.publisher.Mono;

import java.util.function.Function;

public class R2dbcOperationExecutor {


    public <T, R> Mono<R> execute(R2dbcOperationArgs<T, R> args, Function<R2dbcOperationArgs<T, R>, Mono<R>> function) {
        return Mono.just("before")
                .log()
                .flatMap(it -> function.apply(args).log())
                .log()
                .doOnSuccess(it -> System.out.println(it))
                .log();
    }


}
