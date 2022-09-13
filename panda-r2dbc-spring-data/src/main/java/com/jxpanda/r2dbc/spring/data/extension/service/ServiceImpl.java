package com.jxpanda.r2dbc.spring.data.extension.service;

import com.jxpanda.r2dbc.spring.data.core.R2dbcEntityTemplate;
import com.jxpanda.r2dbc.spring.data.extension.Entity;
import com.jxpanda.r2dbc.spring.data.repository.R2dbcRepository;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.data.relational.repository.support.MappingRelationalEntityInformation;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;


public class ServiceImpl<ID, T extends Entity<ID>> implements Service<ID, T> {

    protected final R2dbcEntityTemplate r2dbcEntityTemplate;

    private final R2dbcRepository<T, ID> repository;

    private NamingStrategy namingStrategy;

    public ServiceImpl(R2dbcEntityTemplate r2dbcEntityTemplate) {
        this.r2dbcEntityTemplate = r2dbcEntityTemplate;
        RelationalPersistentEntity<T> requiredPersistentEntity = (RelationalPersistentEntity<T>) r2dbcEntityTemplate.getMappingContext().getRequiredPersistentEntity(getEntityClass());
        MappingRelationalEntityInformation<T, ID> entity = new MappingRelationalEntityInformation<>(requiredPersistentEntity, getIdClass());
        this.repository = new ServiceRepository<>(entity, r2dbcEntityTemplate, r2dbcEntityTemplate.getConverter());
    }

    public ServiceImpl(R2dbcEntityTemplate r2dbcEntityTemplate, R2dbcRepository<T, ID> repository) {
        this.r2dbcEntityTemplate = r2dbcEntityTemplate;
        this.repository = repository;
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
        return r2dbcEntityTemplate.update(getEntityClass())
                .matching(query)
                .apply(buildUpdate(entity))
                .map(l -> {
                    System.out.println(l);
                    return entity;
                });
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

    @SuppressWarnings("unchecked")
    protected <R extends R2dbcRepository<T, ID>> R getRepository() {
        return (R) repository;
    }

    protected String getTableName() {
        return r2dbcEntityTemplate.getTableName(getEntityClass()).getReference();
    }

    public void forEachColum(T entity, Consumer<ColumInfo> consumer) {
        RelationalPersistentEntity<?> requiredPersistentEntity = r2dbcEntityTemplate.getMappingContext().getRequiredPersistentEntity(getEntityClass());
        requiredPersistentEntity.forEach(property -> {
            try {
                consumer.accept(new ColumInfo(property, property.getGetter().invoke(entity)));
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


}
