package com.jxpanda.r2dbc.spring.data.core.operation;

import com.jxpanda.r2dbc.spring.data.core.enhance.query.page.Pagination;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker.Seeker;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.ReactiveSelectOperation;
import org.springframework.data.relational.core.query.Query;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * @author Panda
 */
public interface R2dbcSelectOperation {

    interface R2dbcSelect<T> extends ReactiveSelectOperation.ReactiveSelect<T>, TerminatingSelect<T>, R2dbcOperation<T, R2dbcSelect<T>> {

        <ID> Mono<T> byId(ID id);

        <ID> Flux<T> byIds(Collection<ID> ids);

        Mono<Pagination<T>> seek(Seeker<T> seeker);

        @NonNull
        @Override
        R2dbcSelectOperation.TerminatingSelect<T> matching(@NonNull Query query);
    }

    interface TerminatingSelect<T> extends ReactiveSelectOperation.TerminatingSelect<T> {

        Mono<Pagination<T>> page(Pageable pageable);

        default Mono<Pagination<T>> page(int page, int size) {
            return page(Pagination.of(page, size));
        }


    }

}
