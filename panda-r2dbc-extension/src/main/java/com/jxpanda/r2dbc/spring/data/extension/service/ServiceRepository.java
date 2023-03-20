package com.jxpanda.r2dbc.spring.data.extension.service;

import com.jxpanda.r2dbc.spring.data.extension.entity.Entity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.NoRepositoryBean;
import reactor.core.publisher.Mono;

@NoRepositoryBean
public interface ServiceRepository<T extends Entity<ID>, ID> extends R2dbcRepository<T, ID> {
//    public ServiceRepository(RelationalEntityInformation<T, ID> entity, R2dbcEntityOperations entityOperations, R2dbcConverter converter) {
//        super(entity, entityOperations, converter);
//    }


    @Query("SELECT :sql FROM :tableName GROUP BY :key")
    <K> Mono<Object> groupBy(String tableName, String sql, String key);

}
