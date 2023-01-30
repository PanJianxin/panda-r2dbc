package com.jxpanda.r2dbc.spring.data.core;

import com.jxpanda.r2dbc.spring.data.core.kit.MappingKit;
import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcSaveOperation;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Collection;
import java.util.stream.Collectors;

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

        R2dbcSaveSupport(ReactiveEntityTemplate template, Class<T> domainType, Query query, @Nullable SqlIdentifier tableName) {
            super(template, domainType, query, tableName);
        }


        @Override
        public Mono<T> save(T object) {
            return isUpdate(object)
                    .flatMap(isUpdate -> isUpdate ? this.template.update(object) : this.template.insert(object));
        }

        @Override
        public Flux<T> batchSave(Collection<T> objectList) {
            return this.transactionalOperator().transactional(
                    Mono.just(objectList)
                            .filter(list -> !ObjectUtils.isEmpty(objectList))
                            .flatMapMany(list -> Flux.fromStream(list.stream()))
                            .flatMap(this::save)
                            .switchIfEmpty(Flux.empty())
            );
        }

        /**
         * 返回是否执行更新操作
         */
        private Mono<Boolean> isUpdate(T object) {
            return Mono.create(sink -> {
                // 判断一下是否有有效的id字段，有id则执行更新操作，没有id则执行插入操作
                RelationalPersistentEntity<T> requiredEntity = MappingKit.getRequiredEntity(object);
                RelationalPersistentProperty idProperty = requiredEntity.getIdProperty();
                sink.success(MappingKit.isPropertyEffective(object, requiredEntity, idProperty));
            });
        }

    }


}
