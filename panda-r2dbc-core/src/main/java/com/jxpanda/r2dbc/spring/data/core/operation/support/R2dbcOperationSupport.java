package com.jxpanda.r2dbc.spring.data.core.operation.support;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.core.operation.executor.R2dbcOperationExecutor;
import com.jxpanda.r2dbc.spring.data.core.operation.executor.R2dbcOperationParameter;
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
        private final R2dbcOperationParameter<T, T> operationParameter;

        protected R2dbcSupport(R2dbcOperationParameter<T, T> operationParameter) {
            this.operationParameter = operationParameter;
        }

        protected R2dbcSupport(R2dbcOperationParameter.R2dbcOperationParameterBuilder<T, T> parameterBuilder) {
            this.operationParameter = parameterBuilder.build();
        }

        protected R2dbcSupport(ReactiveEntityTemplate template, Class<T> domainType) {
            this.operationParameter = R2dbcOperationParameter.<T, T>builder()
                    .template(template)
                    .domainType(domainType)
                    .build();
        }

        protected R2dbcOperationParameter.R2dbcOperationParameterBuilder<T, T> rebuild() {
            return this.operationParameter.rebuild(this.operationParameter.getDomainType(), this.operationParameter.getDomainType());
        }

        protected <NT, NR> R2dbcOperationParameter.R2dbcOperationParameterBuilder<NT, NR> rebuild(Class<NT> domainType, Class<NR> returnType) {
            return this.operationParameter.rebuild(domainType, returnType);
        }


        protected static <T, R, S extends R2dbcSupport<T>> S newSupport(R2dbcOperationParameter.R2dbcOperationParameterBuilder<T, R> executorBuilder, Function<R2dbcOperationParameter<T, R>, S> supportBuilder) {
            return supportBuilder.apply(executorBuilder.build());
        }

        protected <R> R2dbcOperationParameter<T, R> operationParameter(Class<R> returnType) {
            return this.operationParameter.rebuild(this.operationParameter.getDomainType(), returnType).build();
        }

        protected <R, E extends R2dbcOperationExecutor<T, R>, B extends R2dbcOperationExecutor.R2dbcExecutorBuilder<T, R, E, B>> B executorBuilder(Supplier<B> builderSupplier) {
            return builderSupplier.get()
                    .operationParameter((R2dbcOperationParameter<T, R>) this.operationParameter);
        }


        protected TransactionalOperator transactionalOperator(int propagationBehavior, int isolationLevel, int timeout, boolean readOnly) {
            return TransactionalOperator.create(this.operationParameter.getTemplate().getR2dbcTransactionManager(), new TransactionDefinition() {
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


