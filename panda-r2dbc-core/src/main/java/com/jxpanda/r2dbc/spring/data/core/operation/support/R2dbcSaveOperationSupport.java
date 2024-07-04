package com.jxpanda.r2dbc.spring.data.core.operation.support;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcSaveOperation;
import com.jxpanda.r2dbc.spring.data.core.operation.executor.R2dbcOperationParameter;
import com.jxpanda.r2dbc.spring.data.core.operation.executor.R2dbcSaveExecutor;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * @author Panda
 */
public class R2dbcSaveOperationSupport extends R2dbcOperationSupport implements R2dbcSaveOperation {
    public R2dbcSaveOperationSupport(ReactiveEntityTemplate template) {
        super(template);
    }

    @Override
    public <T> R2dbcSave<T> save(Class<T> domainType) {

        Assert.notNull(domainType, "DomainType must not be null");

        return new R2dbcSaveSupport<>(R2dbcOperationParameter.<T, T>builder()
                .template(template)
                .domainType(domainType)
                .returnType(domainType)
                .build());
    }


    private static final class R2dbcSaveSupport<T> extends R2dbcSupport<T> implements R2dbcSaveOperation.R2dbcSave<T> {


        private R2dbcSaveSupport(R2dbcOperationParameter<T, T> operationParameter) {
            super(operationParameter);
        }

        @Override
        public Mono<T> using(T entity) {
            return executorBuilder(R2dbcSaveExecutor::builder)
                    .build()
                    .execute(entity);
        }

        @Override
        public Flux<T> batch(Collection<T> entityList) {
            return executorBuilder(R2dbcSaveExecutor::builder)
                    .build()
                    .executeBatch(entityList);
        }


    }


}
