package com.jxpanda.r2dbc.spring.data.extension.service;

import com.jxpanda.r2dbc.spring.data.core.R2dbcEntityTemplate;
import com.jxpanda.r2dbc.spring.data.extension.Entity;
import com.jxpanda.r2dbc.spring.data.repository.R2dbcRepository;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.repository.support.MappingRelationalEntityInformation;
import reactor.core.publisher.Mono;


public class ServiceImpl<ID, T extends Entity<ID>> implements Service<ID, T> {

    protected final R2dbcEntityTemplate r2dbcEntityTemplate;

    protected final R2dbcRepository<T, ID> repository;

    public ServiceImpl(R2dbcEntityTemplate r2dbcEntityTemplate) {
        this.r2dbcEntityTemplate = r2dbcEntityTemplate;
        RelationalPersistentEntity<T> requiredPersistentEntity = (RelationalPersistentEntity<T>) r2dbcEntityTemplate.getMappingContext().getRequiredPersistentEntity(getEntityClass());
        MappingRelationalEntityInformation<T, ID> entity = new MappingRelationalEntityInformation<>(requiredPersistentEntity, getIdClass());
        this.repository = new ServiceRepository<>(entity, r2dbcEntityTemplate, r2dbcEntityTemplate.getConverter());
    }

    protected Class<T> getEntityClass() {
        return null;
    }


    protected Class<ID> getIdClass() {
        return null;
    }

    @Override
    public Mono<T> insert(T entity) {
        return r2dbcEntityTemplate.insert(entity);
    }

    @Override
    public Mono<T> updateById(T entity) {
        return r2dbcEntityTemplate.update(entity);
    }

    @Override
    public Mono<T> update(T entity, Query query) {
//        r2dbcEntityTemplate.update(query,Update.update("",""),entity.getClass());
        return null;
    }

    @Override
    public Mono<T> save(T entity) {
        return null;
    }

    @Override
    public Mono<T> selectById(ID id) {
        return repository.findById(id);
    }

    @Override
    public Mono<T> selectOne(Query query) {
        return null;
    }
}
