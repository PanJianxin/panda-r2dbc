package com.jxpanda.r2dbc.spring.data.core.operation.executor;

import com.jxpanda.r2dbc.spring.data.core.kit.R2dbcMappingKit;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public class R2dbcSaveExecutor<T> extends R2dbcOperationExecutor.WriteExecutor<T, T> {

    private R2dbcSaveExecutor(R2dbcOperationContext<T, T> operationContext, Function<R2dbcOperationContext<T, T>, Query> queryHandler) {
        super(operationContext, queryHandler);
    }

    public static <T> R2dbcSaveExecutorBuilder<T> builder() {
        return new R2dbcSaveExecutorBuilder<>();
    }

    @Override
    protected Mono<T> fetch(R2dbcOperationContext<T, T> operationContext) {
        T entity = operationContext.getEntity();
        return Mono.just(R2dbcMappingKit.isIdEffective(entity))
                .map(isUpdate -> isUpdate ? createUpdateExecutor() : createInertExecutor())
                .flatMap(executor -> executor.execute(entity));
    }

    private R2dbcInsertExecutor<T> createInertExecutor() {
        return swap(R2dbcInsertExecutor::builder)
                .build();
    }

    private R2dbcUpdateExecutor<T, T> createUpdateExecutor() {
        return swap(R2dbcUpdateExecutor::builder).build();
    }

    public static class R2dbcSaveExecutorBuilder<T> extends R2dbcOperationExecutor.R2dbcExecutorBuilder<T, T, R2dbcSaveExecutor<T>, R2dbcSaveExecutorBuilder<T>> {

        @Override
        protected R2dbcSaveExecutorBuilder<T> self() {
            return this;
        }

        @Override
        public R2dbcSaveExecutor<T> buildExecutor() {
            return new R2dbcSaveExecutor<>(operationContext, queryHandler);
        }
    }

}
