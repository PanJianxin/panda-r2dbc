package com.jxpanda.r2dbc.spring.data.extension.service;

import com.jxpanda.r2dbc.spring.data.core.enhance.query.page.Page;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.page.Pagination;
import com.jxpanda.r2dbc.spring.data.extension.entity.Entity;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * @author Panda
 */
public interface Service<T extends Entity<ID>, ID> {

    /**
     * 插入一条数据
     *
     * @param entity entity
     * @return 最新的 entity
     */
    Mono<T> insert(T entity);

    /**
     * 批量创建
     *
     * @param entities entity列表
     * @return 保存后的集合
     */
    Flux<T> insertBatch(Collection<T> entities);

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
     * 批量保存
     * 有ID则更新，无ID则创建
     *
     * @param entities entity列表
     * @return 保存后的集合
     */
    Flux<T> saveBatch(Collection<T> entities);


    /**
     * 基于id删除一条数据
     *
     * @param id id
     * @return 成功/失败
     */
    Mono<Boolean> deleteById(ID id);

    /**
     * 基于id集合批量删除数据
     *
     * @param ids id集合
     * @return 影响了多少条数据
     */
    Mono<Long> deleteByIds(Collection<ID> ids);

    /**
     * 基于条件删除（安全起见，如果条件为空，会拒绝执行）
     *
     * @param query 查询条件
     * @return 影响了多少条数据
     */
    Mono<Long> delete(Query query);

    /**
     * 基于id物理删除一条数据，即便开启了逻辑删除，也执行物理删除
     *
     * @param id id
     * @return 成功/失败
     */
    Mono<Boolean> destroyById(ID id);

    /**
     * 基于id集合批量删除数据，即便开启了逻辑删除，也执行物理删除
     *
     * @param ids id集合
     * @return 影响了多少条数据
     */
    Mono<Long> destroyByIds(Collection<ID> ids);

    /**
     * 基于条件删除（安全起见，如果条件为空，会拒绝执行），即便开启了逻辑删除，也执行物理删除
     *
     * @param query 查询条件
     * @return 影响了多少条数据
     */
    Mono<Long> destroy(Query query);

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

    /**
     * 使用id集合查询列表
     *
     * @param ids id集合
     * @return 数据列表
     */
    Flux<T> listByIds(Collection<ID> ids);

    /**
     * 查询列表
     *
     * @param query query对象
     * @return 数据列表
     */
    Flux<T> list(Query query);

    /**
     * 分页查询
     *
     * @param query 查询条件
     * @param page  分页对象
     * @return 分页后的数据
     */
    Pagination<T> paging(Query query, Page page);

}
