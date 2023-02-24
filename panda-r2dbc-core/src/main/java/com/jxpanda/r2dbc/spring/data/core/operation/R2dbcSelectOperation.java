package com.jxpanda.r2dbc.spring.data.core.operation;

import com.jxpanda.r2dbc.spring.data.core.enhance.query.page.Page;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.page.Pagination;
import org.springframework.data.r2dbc.core.ReactiveSelectOperation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface R2dbcSelectOperation {

    interface R2dbcSelect<T> extends ReactiveSelectOperation.ReactiveSelect<T> {

        <ID> Mono<T> byId(ID id);

        <ID> Flux<T> byIds(Collection<ID> ids);

        Mono<Pagination<T>> paging();

        Mono<Pagination<T>> paging(Page page);

    }

}
