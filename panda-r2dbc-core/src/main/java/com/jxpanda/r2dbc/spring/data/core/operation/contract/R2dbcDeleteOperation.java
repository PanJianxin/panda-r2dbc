package com.jxpanda.r2dbc.spring.data.core.operation.contract;

import org.springframework.data.r2dbc.core.ReactiveDeleteOperation;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * 删除操作，基于配置来区分是逻辑删除还是物理删除
 */
public interface R2dbcDeleteOperation {

    <T> R2dbcDelete<T> delete(@NonNull Class<T> entityType);

    interface R2dbcDelete<T> extends ReactiveDeleteOperation.ReactiveDelete {

        Mono<Boolean> using(T entity);

        <ID> Mono<Boolean> byId(ID id);

        <ID> Mono<Long> byIds(Collection<ID> ids);

    }

}
