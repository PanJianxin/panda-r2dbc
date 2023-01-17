package com.jxpanda.r2dbc.spring.data.core.operation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * 保存操作
 * 逻辑上有主键则更新，没有主键则插入
 */
public interface R2dbcSaveOperation {

    interface R2dbcSave<T> {
        /**
         * 保存一条数据，有id则更新，没有id则创建
         */
        Mono<T> save(T object);

        Flux<T> batchSave(Collection<T> objectList);

    }

}
