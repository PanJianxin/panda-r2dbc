package com.jxpanda.r2dbc.spring.data.extension.service;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.page.Page;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.page.Pagination;
import com.jxpanda.r2dbc.spring.data.core.kit.R2dbcMappingKit;
import com.jxpanda.r2dbc.spring.data.extension.entity.Entity;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.ReflectionKit;
import lombok.Getter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.r2dbc.repository.support.SimpleR2dbcRepository;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;


@SuppressWarnings("unchecked")
public class ServiceImpl<T extends Entity<ID>, ID> implements Service<T, ID> {

    @Autowired
    protected ReactiveEntityTemplate reactiveEntityTemplate;

    @Autowired
    private R2dbcEntityTemplate r2dbcEntityTemplate;

    private R2dbcRepository<T, ID> repository;

    private final Class<ID> idClass;

    private final Class<T> entityClass;

    protected ServiceImpl() {
        this.idClass = takeIdClass();
        this.entityClass = takeEntityClass();
    }

    @Override
    public Mono<T> insert(T entity) {
        return reactiveEntityTemplate.insert(entity);
    }

    @Override
    public Flux<T> insertBatch(Collection<T> entities) {
        return reactiveEntityTemplate.insertBatch(entities, this.entityClass);
    }

    @Override
    public Mono<T> updateById(T entity) {
        return reactiveEntityTemplate.update(entity);
    }

    @Override
    public Mono<T> update(T entity, Query query) {
        return reactiveEntityTemplate.update(this.idClass)
                .matching(query)
                .apply(buildUpdate(entity))
                .thenReturn(entity);
    }

    @Override
    public Mono<T> save(T entity) {
        return reactiveEntityTemplate.save(entity);
    }

    @Override
    public Flux<T> saveBatch(Collection<T> entities) {
        return reactiveEntityTemplate.saveBatch(entities, this.entityClass);
    }

    @Override
    public Mono<Boolean> deleteById(ID id) {
        return reactiveEntityTemplate.delete(this.entityClass)
                .matching(Query.query(Criteria.where(Entity.ID).is(id)))
                .all()
                .map(it -> it > 0);
    }

    @Override
    public Mono<Long> deleteByIds(Collection<ID> ids) {
        return reactiveEntityTemplate.delete(this.entityClass)
                .matching(Query.query(Criteria.where(Entity.ID).in(ids)))
                .all();
    }

    @Override
    public Mono<Long> delete(Query query) {
        return reactiveEntityTemplate.delete(query, this.entityClass);
    }

    @Override
    public Mono<Boolean> destroyById(ID id) {
        return reactiveEntityTemplate.destroy(this.entityClass)
                .matching(Query.query(Criteria.where(Entity.ID).is(id)))
                .all()
                .map(it -> it > 0);
    }

    @Override
    public Mono<Long> destroyByIds(Collection<ID> ids) {
        return reactiveEntityTemplate.destroy(this.entityClass)
                .matching(Query.query(Criteria.where(Entity.ID).in(ids)))
                .all();
    }

    @Override
    public Mono<Long> destroy(Query query) {
        return reactiveEntityTemplate.destroy(query, this.entityClass);
    }

    @Override
    public Mono<T> selectById(ID id) {
        return this.getRepository().findById(id);
    }

    @Override
    public Mono<T> selectOne(Query query) {
        return reactiveEntityTemplate.select(this.entityClass).matching(query).one();
    }

    @Override
    public Flux<T> listByIds(Collection<ID> ids) {
        return null;
    }

    @Override
    public Flux<T> list(Query query) {
        return null;
    }

    @Override
    public Pagination<T> paging(Query query, Page page) {
        return null;
    }

    @SuppressWarnings({"unchecked", "unused", "SameParameterValue"})
    protected <R extends R2dbcRepository<T, ID>> R getRepository() {
        if (repository == null) {
            this.repository = new SimpleR2dbcRepository<>(R2dbcMappingKit.getMappingRelationalEntityInformation(this.entityClass, this.idClass), r2dbcEntityTemplate, reactiveEntityTemplate.getConverter());
        }
        return (R) repository;
    }

    protected String getTableName() {
        return getMappingContext().getRequiredPersistentEntity(this.entityClass).getTableName().getReference();
    }

    public void forEachColum(T entity, Consumer<ColumInfo> consumer) {
        RelationalPersistentEntity<?> requiredPersistentEntity = getMappingContext().getRequiredPersistentEntity(this.entityClass);
        requiredPersistentEntity.forEach(property -> {
            try {
                if (property != null && property.getGetter() != null) {
                    consumer.accept(new ColumInfo(property, property.getGetter().invoke(entity)));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Update buildUpdate(T entity) {
        return buildUpdate(entity, columInfo -> columInfo.getValue() != null);
    }

    private Update buildUpdate(T entity, Predicate<ColumInfo> predicate) {
        Update update = Update.from(new HashMap<>());
        forEachColum(entity, columInfo -> {
            if (predicate.test(columInfo)) {
                update.set(columInfo.getColumName(), columInfo.getValue());
            }
        });
        return update;
    }

    @Getter
    @ToString
    public static class ColumInfo {

        private final String fieldName;
        private final String columName;
        private final String alias;
        private final Object value;

        private ColumInfo(RelationalPersistentProperty property, Object value) {
            this.fieldName = property.getName();
            this.value = value;
            this.alias = "";
            this.columName = property.getColumnName().getReference();
        }

    }

    private MappingContext<? extends RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> getMappingContext() {
        return reactiveEntityTemplate.getConverter().getMappingContext();
    }

    private Class<ID> takeIdClass() {
        return takeGenericType(1);
    }

    private Class<T> takeEntityClass() {
        return takeGenericType(0);
    }

    private <X> Class<X> takeGenericType(int index) {
        return (Class<X>) ReflectionKit.getSuperClassGenericType(this.getClass(), ServiceImpl.class, index);
    }

}
