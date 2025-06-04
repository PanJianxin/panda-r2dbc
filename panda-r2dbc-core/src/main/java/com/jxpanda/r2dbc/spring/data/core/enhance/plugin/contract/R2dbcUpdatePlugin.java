package com.jxpanda.r2dbc.spring.data.core.enhance.plugin.contract;

import org.springframework.data.relational.core.query.Update;
import reactor.core.publisher.Mono;

public interface R2dbcUpdatePlugin<T> extends R2dbcBeforePlugin {

    /** 处理 Update，返回新的 Update */
    Mono<Update> handleUpdate(Class<T> entityType, Update original);

}
