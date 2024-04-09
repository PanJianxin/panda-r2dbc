package com.jxpanda.r2dbc.spring.data.core.operation.executor;

import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcPluginName;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public class R2dbcDestroyExecutor<T, R> extends R2dbcOperationExecutor.WriteExecutor<T, R> {

    private R2dbcDestroyExecutor(R2dbcOperationParameter<T, R> operationParameter, Function<R2dbcOperationParameter<T, R>, Query> queryHandler) {
        super(operationParameter, queryHandler);
    }

    public static <T, R> R2dbcDestroyExecutorBuilder<T, R> builder() {
        return new R2dbcDestroyExecutorBuilder<>();
    }

    @Override
    protected Mono<R> fetch(T domainEntity, R2dbcOperationParameter<T, R> parameter) {
        // 物理删除就是强制禁用逻辑删除就行了
        parameter.getOption().disablePlugin(R2dbcPluginName.LOGIC_DELETE);
        return swap(R2dbcDeleteExecutor::builder)
                .build()
                .fetch(domainEntity, parameter);
    }

    @Override
    protected Mono<R> fetch(R2dbcOperationParameter<T, R> parameter) {
        // 物理删除就是强制禁用逻辑删除就行了
        parameter.getOption().disablePlugin(R2dbcPluginName.LOGIC_DELETE);
        return swap(R2dbcDeleteExecutor::builder)
                .build()
                .fetch(parameter);
    }

    public static class R2dbcDestroyExecutorBuilder<T, R> extends R2dbcOperationExecutor.R2dbcExecutorBuilder<T, R, R2dbcDestroyExecutor<T, R>, R2dbcDestroyExecutorBuilder<T, R>> {
        @Override
        protected R2dbcDestroyExecutorBuilder<T, R> self() {
            return this;
        }

        @Override
        public R2dbcDestroyExecutor<T, R> buildExecutor() {
            return new R2dbcDestroyExecutor<>(operationParameter, queryHandler);
        }
    }

}
