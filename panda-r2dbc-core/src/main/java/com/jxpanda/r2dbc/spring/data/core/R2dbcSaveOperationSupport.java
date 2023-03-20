package com.jxpanda.r2dbc.spring.data.core;

import com.jxpanda.r2dbc.spring.data.core.kit.R2dbcMappingKit;
import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcSaveOperation;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
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

        return new R2dbcSaveSupport<>(this.template, domainType);
    }


    private final static class R2dbcSaveSupport<T> extends R2dbcSupport<T> implements R2dbcSaveOperation.R2dbcSave<T> {

        R2dbcSaveSupport(ReactiveEntityTemplate template, Class<T> domainType) {
            super(template, domainType);
        }

        @Override
        public Mono<T> using(T object) {
            return isUpdate(object)
                    .flatMap(isUpdate -> isUpdate ? this.reactiveEntityTemplate.update(object) : this.reactiveEntityTemplate.insert(object));
        }

        @Override
        public Flux<T> batch(Collection<T> objectList) {
            return Mono.just(objectList)
                    .filter(list -> !ObjectUtils.isEmpty(objectList))
                    .flatMapMany(Flux::fromIterable)
                    .flatMap(this::using)
                    .switchIfEmpty(Flux.empty())
                    .as(this.transactionalOperator()::transactional);
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

    }


}
