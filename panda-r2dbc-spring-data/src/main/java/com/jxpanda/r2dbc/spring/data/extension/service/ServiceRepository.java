package com.jxpanda.r2dbc.spring.data.extension.service;

import com.jxpanda.r2dbc.spring.data.convert.R2dbcConverter;
import com.jxpanda.r2dbc.spring.data.core.expander.R2dbcDataAccessStrategy;
import com.jxpanda.r2dbc.spring.data.core.operation.R2dbcEntityOperations;
import com.jxpanda.r2dbc.spring.data.extension.Entity;
import com.jxpanda.r2dbc.spring.data.repository.R2dbcRepository;
import com.jxpanda.r2dbc.spring.data.repository.support.SimpleR2dbcRepository;
import org.springframework.data.relational.repository.query.RelationalEntityInformation;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

public class ServiceRepository<ID,T extends Entity<ID>> extends SimpleR2dbcRepository<T, ID> {
    public ServiceRepository(RelationalEntityInformation<T, ID> entity, R2dbcEntityOperations entityOperations, R2dbcConverter converter) {
        super(entity, entityOperations, converter);
    }

    public ServiceRepository(RelationalEntityInformation<T, ID> entity, DatabaseClient databaseClient, R2dbcConverter converter, R2dbcDataAccessStrategy accessStrategy) {
        super(entity, databaseClient, converter, accessStrategy);
    }
}
