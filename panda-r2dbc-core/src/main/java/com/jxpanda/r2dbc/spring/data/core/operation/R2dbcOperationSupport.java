package com.jxpanda.r2dbc.spring.data.core.operation;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.core.operation.executor.R2dbcOperationExecutor;
import com.jxpanda.r2dbc.spring.data.core.operation.executor.R2dbcOperationContext;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionalOperator;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Panda
 */
public class R2dbcOperationSupport {


    protected final ReactiveEntityTemplate template;


    public R2dbcOperationSupport(ReactiveEntityTemplate template) {
        this.template = template;
    }


    @SuppressWarnings({"unchecked"})
    protected static class R2dbcSupport<T> {
        private final R2dbcOperationContext<T, T> operationContext;

        protected R2dbcSupport(R2dbcOperationContext<T, T> operationContext) {
            this.operationContext = operationContext;
        }

        protected R2dbcSupport(R2dbcOperationContext.R2dbcOperationContextBuilder<T, T> contextBuilder) {
            this.operationContext = contextBuilder.build();
        }

        protected R2dbcSupport(ReactiveEntityTemplate template, Class<T> entityType) {
            this.operationContext = R2dbcOperationContext.<T, T>builder()
                    .template(template)
                    .entityType(entityType)
                    .build();
        }

        protected R2dbcOperationContext.R2dbcOperationContextBuilder<T, T> rebuilder() {
            return this.operationContext.rebuilder(this.operationContext.getEntityType(), this.operationContext.getEntityType());
        }

        protected <NT, NR> R2dbcOperationContext.R2dbcOperationContextBuilder<NT, NR> rebuilder(Class<NT> entityType, Class<NR> resultType) {
            return this.operationContext.rebuilder(entityType, resultType);
        }


        protected static <T, R, S extends R2dbcSupport<T>> S newSupport(R2dbcOperationContext.R2dbcOperationContextBuilder<T, R> contextBuilder, Function<R2dbcOperationContext<T, R>, S> supportBuilder) {
            return supportBuilder.apply(contextBuilder.build());
        }

        protected <R> R2dbcOperationContext<T, R> operationContext(Class<R> resultType) {
            return this.operationContext.rebuilder(this.operationContext.getEntityType(), resultType).build();
        }

        protected <R, E extends R2dbcOperationExecutor<T, R>, B extends R2dbcOperationExecutor.R2dbcExecutorBuilder<T, R, E, B>> B executorBuilder(Supplier<B> builderSupplier) {
            return builderSupplier.get()
                    .operationContext((R2dbcOperationContext<T, R>) this.operationContext);
        }


        protected TransactionalOperator transactionalOperator(int propagationBehavior, int isolationLevel, int timeout, boolean readOnly) {
            return TransactionalOperator.create(this.operationContext.getTemplate().getR2dbcTransactionManager(), new TransactionDefinition() {
                @Override
                public int getPropagationBehavior() {
                    return propagationBehavior;
                }

                @Override
                public int getIsolationLevel() {
                    return isolationLevel;
                }

                @Override
                public int getTimeout() {
                    return timeout;
                }

                @Override
                public boolean isReadOnly() {
                    return readOnly;
                }
            });
        }

    }

}


