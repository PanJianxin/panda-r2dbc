package com.jxpanda.r2dbc.spring.data.extension.service;

import com.jxpanda.r2dbc.spring.data.core.*;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.page.Pagination;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker.Seeker;
import com.jxpanda.r2dbc.spring.data.core.operation.*;
import com.jxpanda.r2dbc.spring.data.core.operation.contract.*;
import com.jxpanda.r2dbc.spring.data.extension.entity.Entity;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Panda
 */
public interface ReactiveEntityRepository<T extends Entity> {

    ReactiveEntityTemplate getReactiveEntityTemplate();

    Class<T> getEntityClass();

    default R2dbcSelectOperation.R2dbcSelect<T> select() {
        return new R2dbcSelectOperationSupport(getReactiveEntityTemplate())
                .select(getEntityClass());
    }

    default R2dbcInsertOperation.R2dbcInsert<T> insert() {
        return new R2dbcInsertOperationSupport(getReactiveEntityTemplate())
                .insert(getEntityClass());
    }

    default R2dbcUpdateOperation.R2dbcUpdate<T> update() {
        return new R2dbcUpdateOperationSupport(getReactiveEntityTemplate())
                .update(getEntityClass());
    }

    default R2dbcSaveOperation.R2dbcSave<T> save() {
        return new R2dbcSaveOperationSupport(getReactiveEntityTemplate())
                .save(getEntityClass());
    }

    default R2dbcDeleteOperation.R2dbcDelete<T> delete() {
        return new R2dbcDeleteOperationSupport(getReactiveEntityTemplate())
                .delete(getEntityClass());
    }

    default R2dbcDestroyOperation.R2dbcDestroy<T> destroy() {
        return new R2dbcDestroyOperationSupport(getReactiveEntityTemplate())
                .destroy(getEntityClass());
    }

    /**
     * 插入一条数据
     *
     * @param entity entity
     * @return 最新的 entity
     */
    default Mono<T> insert(T entity) {
        return insert().using(entity);
    }

    /**
     * 批量创建
     *
     * @param entities entity列表
     * @return 保存后的集合
     */
    default Flux<T> insertBatch(Collection<T> entities) {
        return insert().batch(entities);
    }

    /**
     * 基于String更新一条数据
     *
     * @param entity entity
     * @return 最新的 entity
     */
    default Mono<T> updateById(T entity) {
        return update().using(entity);
    }

    /**
     * 基于条件更新
     *
     * @param entity entity
     * @param query  query
     * @return 更新后的对象
     */
    default Mono<T> update(T entity, Query query) {
        return update()
                .matching(query)
                .apply(ReactiveEntityRepositoryHelper.buildUpdate(entity))
                .thenReturn(entity);
    }

    /**
     * 保存一条数据
     * 有String则更新，无String则创建
     *
     * @param entity entity
     * @return 最新的 entity
     */
    default Mono<T> save(T entity) {
        return save().using(entity);
    }


    /**
     * 批量保存
     * 有String则更新，无String则创建
     *
     * @param entities entity列表
     * @return 保存后的集合
     */
    default Flux<T> saveBatch(Collection<T> entities) {
        return save().batch(entities);
    }


    /**
     * 基于id删除一条数据
     *
     * @param id id
     * @return 成功/失败
     */
    default Mono<Boolean> deleteById(String id) {
        return delete().byId(id);
    }

    /**
     * 基于id集合批量删除数据
     *
     * @param ids id集合
     * @return 影响了多少条数据
     */
    default Mono<Long> deleteByIds(Collection<String> ids) {
        return delete().byIds(ids);
    }

    /**
     * 基于条件删除（安全起见，如果条件为空，会拒绝执行）
     *
     * @param query 查询条件
     * @return 影响了多少条数据
     */
    default Mono<Long> delete(Query query) {
        return delete().matching(query).all();
    }

    /**
     * 基于id物理删除一条数据，即便开启了逻辑删除，也执行物理删除
     *
     * @param id id
     * @return 成功/失败
     */
    default Mono<Boolean> destroyById(String id) {
        return destroy().byId(id);
    }

    /**
     * 基于id集合批量删除数据，即便开启了逻辑删除，也执行物理删除
     *
     * @param ids id集合
     * @return 影响了多少条数据
     */
    default Mono<Long> destroyByIds(Collection<String> ids) {
        return destroy().byIds(ids);
    }

    /**
     * 基于条件删除（安全起见，如果条件为空，会拒绝执行），即便开启了逻辑删除，也执行物理删除
     *
     * @param query 查询条件
     * @return 影响了多少条数据
     */
    default Mono<Long> destroy(Query query) {
        return destroy().matching(query).all();
    }

    /**
     * 使用String查询1条数据
     *
     * @param id id
     * @return 查询到的entity
     */
    default Mono<T> selectById(String id) {
        return select().byId(id);
    }

    /**
     * 使用查询条件查询1条数据
     *
     * @param query 查询条件
     * @return 查询到的entity
     */
    default Mono<T> selectOne(Query query) {
        return select().matching(query).one();
    }

    /**
     * 使用id集合查询列表
     *
     * @param ids id集合
     * @return 数据列表
     */
    default Flux<T> listByIds(Collection<String> ids) {
        return select().byIds(ids);
    }

    /**
     * 查询列表
     *
     * @param query query对象
     * @return 数据列表
     */
    default Flux<T> list(Query query) {
        return select().matching(query).all();
    }

    /**
     * 使用id集合查询列表，然后映射成一个Map结构
     *
     * @param ids id列表
     * @return map结构的数据，key是id，value是entity
     */
    default Mono<Map<String, T>> associateByIds(Collection<String> ids) {
        return select().byIds(ids)
                .collect(Collectors.toMap(Entity::getId, Function.identity()));
    }

    /**
     * 查询列表，然后映射成一个Map结构
     *
     * @param keySelector keySelector
     * @param query       查询条件
     * @return map结构的数据，key是id，value是entity
     */
    default <K> Mono<Map<K, T>> associateBy(Function<? super T, ? extends K> keySelector, Query query) {
        return select()
                .matching(query)
                .all()
                .collect(Collectors.toMap(keySelector, Function.identity()));
    }


    /**
     * 分组查询列表
     *
     * @param keySelector keySelector
     * @param query       查询条件
     * @return map结构的数据，key是id，value是entity list
     */
    default <K> Mono<Map<K, List<T>>> groupBy(Function<? super T, ? extends K> keySelector, Query query) {
        return select()
                .matching(query)
                .all()
                .collect(Collectors.groupingBy(keySelector));
    }

    /**
     * 分页查询
     *
     * @param seeker 查询条件
     * @return 分页后的数据
     */
    default Mono<Pagination<T>> seek(Seeker<T> seeker) {
        return select().seek(seeker);
    }


}
