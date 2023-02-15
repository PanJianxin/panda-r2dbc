package com.jxpanda.r2dbc.spring.data.core.operation;

import com.jxpanda.r2dbc.spring.data.core.enhance.query.page.Page;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.page.Pagination;
import org.springframework.data.r2dbc.core.ReactiveSelectOperation;
import reactor.core.publisher.Mono;

public interface R2dbcSelectOperation {

    interface R2dbcSelect<T> extends ReactiveSelectOperation.ReactiveSelect<T> {

        Mono<Pagination<T>> paging();

        Mono<Pagination<T>> paging(Page page);

    }

}
