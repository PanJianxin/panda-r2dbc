package com.jxpanda.r2dbc.spring.data.core.operation;

import org.springframework.data.r2dbc.core.ReactiveDeleteOperation;
import reactor.core.publisher.Mono;

/**
 * 删除操作，基于配置来区分是逻辑删除还是物理删除
 * */
public interface R2dbcDeleteOperation extends ReactiveDeleteOperation {

    interface R2dbcDelete extends ReactiveDeleteOperation.ReactiveDelete {

        <T> Mono<Boolean> using(T entity);

    }

}
