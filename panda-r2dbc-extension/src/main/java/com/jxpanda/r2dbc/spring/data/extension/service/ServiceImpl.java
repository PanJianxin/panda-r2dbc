package com.jxpanda.r2dbc.spring.data.extension.service;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.core.kit.R2dbcMappingKit;
import com.jxpanda.r2dbc.spring.data.extension.entity.Entity;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.r2dbc.repository.support.SimpleR2dbcRepository;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.repository.support.MappingRelationalEntityInformation;

@Getter
public class ServiceImpl<T extends Entity<ID>, ID> implements Service<T, ID> {

    @Autowired
    protected ReactiveEntityTemplate reactiveEntityTemplate;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private R2dbcRepository<T, ID> repository;

    private String tableName;

    private final Class<ID> idClass = ServiceHelper.takeIdClass(this.getClass());

    private final Class<T> entityClass = ServiceHelper.takeEntityClass(this.getClass());

    public <R extends R2dbcRepository<T, ID>> R getRepository(Class<R> clazz) {
        if (repository == null) {
            MappingRelationalEntityInformation<T, ID> entityInformation = R2dbcMappingKit.getMappingRelationalEntityInformation(this.entityClass, this.idClass);
            this.repository = new SimpleR2dbcRepository<>(entityInformation, reactiveEntityTemplate, reactiveEntityTemplate.getConverter());
        }
        return clazz.cast(this.repository);
    }

    @Override
    public String getTableName() {
        if (tableName == null) {
            tableName = Service.super.getTableName();
        }
        return tableName;
    }
}
