package com.jxpanda.r2dbc.spring.data.core.operation.executor;

import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model.R2dbcPluginEnum;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public class R2dbcDestroyExecutor<T, R> extends R2dbcOperationExecutor.WriteExecutor<T, R> {

    private R2dbcDestroyExecutor(R2dbcOperationContext<T, R> operationContext, Function<R2dbcOperationContext<T, R>, Query> queryHandler) {
        super(operationContext, queryHandler);
    }

    public static <T, R> R2dbcDestroyExecutorBuilder<T, R> builder() {
        return new R2dbcDestroyExecutorBuilder<>();
    }

    @Override
    protected Mono<R> fetch(R2dbcOperationContext<T, R> operationContext) {
        // 物理删除就是强制禁用逻辑删除就行了
        operationContext.getOption().disablePlugin(R2dbcPluginEnum.Name.LOGIC_DELETE.name());
        return swap(R2dbcDeleteExecutor::builder)
                .build()
                .fetch(operationContext);
    }

    public static class R2dbcDestroyExecutorBuilder<T, R> extends R2dbcOperationExecutor.R2dbcExecutorBuilder<T, R, R2dbcDestroyExecutor<T, R>, R2dbcDestroyExecutorBuilder<T, R>> {
        @Override
        protected R2dbcDestroyExecutorBuilder<T, R> self() {
            return this;
        }

        @Override
        public R2dbcDestroyExecutor<T, R> buildExecutor() {
            return new R2dbcDestroyExecutor<>(operationContext, queryHandler);
        }
    }

}
