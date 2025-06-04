package com.jxpanda.r2dbc.spring.data.core.enhance.plugin.contract;

import reactor.core.publisher.Mono;

public interface R2dbcResultPlugin<T, R> extends R2dbcAfterPlugin{

    /**
     * SQL 执行完毕后，会拿到一个返回值 R
     * 这里可以做 null 安全、i18n、加解密、hash 校验等
     */
    Mono<R> handleResult(Class<T> entityType, R originalResult);

}
