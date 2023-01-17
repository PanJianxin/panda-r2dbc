package com.jxpanda.r2dbc.spring.data.core;

import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcSaveOperation;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public class R2dbcSaveOperationSupport extends R2dbcOperationSupport {
    public R2dbcSaveOperationSupport(ReactiveEntityTemplate template) {
        super(template);
    }


    private final static class R2dbcSaveSupport<T> extends R2dbcSupport<T, T> implements R2dbcSaveOperation.R2dbcSave<T> {
        R2dbcSaveSupport(ReactiveEntityTemplate template, Class<T> domainType, Class<T> returnType, Query query, SqlIdentifier tableName) {
            super(template, domainType, returnType, query, tableName);
        }


        @Override
        public Mono<T> save(T object) {
            return null;
        }

        @Override
        public Flux<T> batchSave(Collection<T> objectList) {
            return null;
        }
    }


}
