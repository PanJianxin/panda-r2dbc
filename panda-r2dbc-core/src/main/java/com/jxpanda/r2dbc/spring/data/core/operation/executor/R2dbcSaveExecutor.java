package com.jxpanda.r2dbc.spring.data.core.operation.executor;

import com.jxpanda.r2dbc.spring.data.core.kit.R2dbcMappingKit;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public class R2dbcSaveExecutor<T> extends R2dbcOperationExecutor.WriteExecutor<T, T> {

    private R2dbcSaveExecutor(R2dbcOperationParameter<T, T> operationParameter, Function<R2dbcOperationParameter<T, T>, Query> queryHandler) {
        super(operationParameter, queryHandler);
    }

    public static <T> R2dbcSaveExecutorBuilder<T> builder() {
        return new R2dbcSaveExecutorBuilder<>();
    }

    @Override
    protected Mono<T> fetch(T domainEntity, R2dbcOperationParameter<T, T> parameter) {
        return isUpdate(domainEntity)
                .map(isUpdate -> isUpdate ? createUpdateExecutor() : createInertExecutor())
                .flatMap(executor -> executor.execute(domainEntity));
    }

    @Override
    protected Mono<T> fetch(R2dbcOperationParameter<T, T> parameter) {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    private R2dbcInsertExecutor<T> createInertExecutor() {
        return swap(R2dbcInsertExecutor::builder)
                .build();
    }

    private R2dbcUpdateExecutor<T, T> createUpdateExecutor() {
        return swap(R2dbcUpdateExecutor::builder).build();
    }

    /**
     * 返回是否执行更新操作
     */
    private Mono<Boolean> isUpdate(T object) {
        return Mono.create(sink -> {
            // 判断一下是否有有效的id字段，有id则执行更新操作，没有id则执行插入操作
            RelationalPersistentEntity<T> requiredEntity = R2dbcMappingKit.getRequiredEntity(object);
            RelationalPersistentProperty idProperty = requiredEntity.getIdProperty();
            sink.success(R2dbcMappingKit.isPropertyEffective(object, requiredEntity, idProperty));
        });
    }

    public static class R2dbcSaveExecutorBuilder<T> extends R2dbcOperationExecutor.R2dbcExecutorBuilder<T, T, R2dbcSaveExecutor<T>, R2dbcSaveExecutorBuilder<T>> {

        @Override
        protected R2dbcSaveExecutorBuilder<T> self() {
            return this;
        }

        @Override
        public R2dbcSaveExecutor<T> build() {
            return new R2dbcSaveExecutor<>(operationParameter, queryHandler);
        }
    }

}
