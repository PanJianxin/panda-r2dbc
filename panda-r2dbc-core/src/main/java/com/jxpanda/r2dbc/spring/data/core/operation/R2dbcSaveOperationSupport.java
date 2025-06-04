package com.jxpanda.r2dbc.spring.data.core.operation;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.core.operation.contract.R2dbcSaveOperation;
import com.jxpanda.r2dbc.spring.data.core.operation.executor.R2dbcOperationContext;
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
    public <T> R2dbcSave<T> save(Class<T> entityType) {

        Assert.notNull(entityType, "entityType must not be null");

        return new R2dbcSaveSupport<>(R2dbcOperationContext.<T, T>builder()
                .template(template)
                .entityType(entityType)
                .resultType(entityType)
                .build());
    }


    private static final class R2dbcSaveSupport<T> extends R2dbcSupport<T> implements R2dbcSaveOperation.R2dbcSave<T> {


        private R2dbcSaveSupport(R2dbcOperationContext<T, T> operationContext) {
            super(operationContext);
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
