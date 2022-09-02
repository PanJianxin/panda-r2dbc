package com.jxpanda.r2dbc.spring.data.extension.service;

import com.jxpanda.r2dbc.spring.data.extension.Entity;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Mono;

/**
 * @author Panda
 */
public interface Service<ID, T extends Entity<ID>> {

    /**
     * 插入一条数据
     *
     * @param entity entity
     * @return 最新的 entity
     */
    Mono<T> insert(T entity);

    /**
     * 基于ID更新一条数据
     *
     * @param entity entity
     * @return 最新的 entity
     */
    Mono<T> updateById(T entity);

    /**
     * 基于条件更新
     *
     * @param entity entity
     * @param query  query
     * @return 更新后的对象
     */
    Mono<T> update(T entity, Query query);

    /**
     * 保存一条数据
     * 有ID则更新，无ID则创建
     *
     * @param entity entity
     * @return 最新的 entity
     */
    Mono<T> save(T entity);

    /**
     * 使用ID查询1条数据
     *
     * @param id id
     * @return 查询到的entity
     */
    Mono<T> selectById(ID id);

    /**
     * 使用查询条件查询1条数据
     *
     * @param query 查询条件
     * @return 查询到的entity
     */
    Mono<T> selectOne(Query query);


}
