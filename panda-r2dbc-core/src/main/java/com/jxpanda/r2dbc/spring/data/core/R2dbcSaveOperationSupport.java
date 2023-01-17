package com.jxpanda.r2dbc.spring.data.core;

import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcSaveOperation;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public class R2dbcSaveOperationSupport extends R2dbcOperationSupport implements R2dbcSaveOperation {
    public R2dbcSaveOperationSupport(ReactiveEntityTemplate template) {
        super(template);
    }

    @Override
    public <T> R2dbcSave<T> save(Class<T> domainType) {

        Assert.notNull(domainType, "DomainType must not be null");

        return new R2dbcSaveSupport<>(this.template, domainType, domainType, Query.empty(), null);
    }


    private final static class R2dbcSaveSupport<T> extends R2dbcSupport<T, T> implements R2dbcSaveOperation.R2dbcSave<T> {
        R2dbcSaveSupport(ReactiveEntityTemplate template, Class<T> domainType, Class<T> returnType, Query query, @Nullable SqlIdentifier tableName) {
            super(template, domainType, returnType, query, tableName);
        }


        @Override
        public Mono<T> save(T object) {
            RelationalPersistentEntity<T> requiredEntity = this.coordinator.getRequiredEntity(object);

            return null;
        }

        @Override
        public Flux<T> batchSave(Collection<T> objectList) {
            return null;
        }
    }


}
