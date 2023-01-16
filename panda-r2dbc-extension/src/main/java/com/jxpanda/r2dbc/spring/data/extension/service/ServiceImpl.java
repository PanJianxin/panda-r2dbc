package com.jxpanda.r2dbc.spring.data.extension.service;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.core.query.LambdaCriteria;
import com.jxpanda.r2dbc.spring.data.extension.entity.Entity;
import com.jxpanda.r2dbc.spring.data.extension.entity.StandardEntity;
import com.jxpanda.r2dbc.spring.data.core.enhance.key.IdGenerator;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.ReflectionKit;
import lombok.Getter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
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


@SuppressWarnings("unchecked")
public class ServiceImpl<T extends Entity<ID>, ID> implements Service<T, ID> {

    protected final ReactiveEntityTemplate reactiveEntityTemplate;

    private final R2dbcRepository<T, ID> repository;

    @Autowired
    private NamingStrategy namingStrategy;

    @Autowired
    private IdGenerator<ID> idGenerator;

    private Class<ID> idClass;

    private Class<T> entityClass;

    public ServiceImpl(ReactiveEntityTemplate reactiveEntityTemplate) {
        this.reactiveEntityTemplate = reactiveEntityTemplate;
        RelationalPersistentEntity<T> requiredPersistentEntity = (RelationalPersistentEntity<T>) getMappingContext().getRequiredPersistentEntity(getEntityClass());
        MappingRelationalEntityInformation<T, ID> entity = new MappingRelationalEntityInformation<>(requiredPersistentEntity, getIdClass());
        this.repository = new ServiceRepository<>(entity, reactiveEntityTemplate, reactiveEntityTemplate.getConverter());
    }

    public ServiceImpl(ReactiveEntityTemplate reactiveEntityTemplate, R2dbcRepository<T, ID> repository) {
        this.reactiveEntityTemplate = reactiveEntityTemplate;
        this.repository = repository;
    }

    @Override
    public Mono<T> insert(T entity) {
        return Mono.just(idGenerator)
                .map(IdGenerator::generate)
                .flatMap(id -> {
                    entity.setId(id);
                    return reactiveEntityTemplate.insert(entity);
                });
    }

    @Override
    public Mono<T> updateById(T entity) {
        return reactiveEntityTemplate.update(entity);
    }

    @Override
    public Mono<T> update(T entity, Query query) {
        return reactiveEntityTemplate.update(getEntityClass())
                .matching(query)
                .apply(buildUpdate(entity))
                .map(l -> entity);
    }

    @Override
    public Mono<T> save(T entity) {
        return null;
    }

    @Override
    public Mono<T> selectById(ID id) {
        return this.selectOne(Query.query(LambdaCriteria.where(StandardEntity<ID>::getId).is(id)));
    }

    @Override
    public Mono<T> selectOne(Query query) {
        return reactiveEntityTemplate.select(getEntityClass()).matching(query).one();
    }

    @SuppressWarnings({"unchecked", "unused", "SameParameterValue"})
    protected <R extends R2dbcRepository<T, ID>> R getRepository(Class<R> clazz) {
        return (R) repository;
    }

    protected String getTableName() {
        return getMappingContext().getRequiredPersistentEntity(getEntityClass()).getTableName().getReference();
    }

    public void forEachColum(T entity, Consumer<ColumInfo> consumer) {
        RelationalPersistentEntity<?> requiredPersistentEntity = getMappingContext().getRequiredPersistentEntity(getEntityClass());
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

    protected Class<ID> getIdClass() {
        if (idClass == null) {
            idClass = (Class<ID>) ReflectionKit.getSuperClassGenericType(this.getClass(), ServiceImpl.class, 1);
        }
        return idClass;
    }


    protected Class<T> getEntityClass() {
        if (entityClass == null) {
            entityClass = (Class<T>) ReflectionKit.getSuperClassGenericType(this.getClass(), ServiceImpl.class, 0);
        }
        return entityClass;
    }

    protected NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    protected IdGenerator<ID> getIdGenerator() {
        return idGenerator;
    }

//    protected NullPolicy getNullPolicy(){
//        return
//    }

}
