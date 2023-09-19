package com.jxpanda.r2dbc.spring.data.core.enhance.plugin;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * @author Panda
 */
@AllArgsConstructor
public class R2dbcOperationExecutor<T,R> {

    private final R2dbcOperationArgs<T, R> args;

    public  Mono<R> execute(Function<R2dbcOperationArgs<T, R>, Mono<R>> function) {
        return Mono.just("before")
                .log()
                .flatMap(it -> function.apply(args).log())
                .log()
                .doOnSuccess(System.out::println)
                .log();
    }

//    public <T, R> Flux<R> execute(R2dbcOperationArgs<T, R> args, Function<R2dbcOperationArgs<T, R>, Flux<R>> function) {
//        return Mono.just("before")
//                .log()
//                .flatMapMany(it -> function.apply(args).log())
//                .log();
//    }


}
