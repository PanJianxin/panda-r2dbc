package com.jxpanda.r2dbc.spring.data.core.operation;

import org.springframework.data.r2dbc.core.ReactiveInsertOperation;
import reactor.core.publisher.Flux;

import java.util.Collection;

/**
 * 插入操作
 * 扩展了批量插入
 */
public interface R2dbcInsertOperation extends ReactiveInsertOperation {


    interface R2dbcInsert<T> extends ReactiveInsertOperation.ReactiveInsert<T> {

        Flux<T> batchInsert(Collection<T> objectList);

    }

}
