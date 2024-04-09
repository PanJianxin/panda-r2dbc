package com.jxpanda.r2dbc.spring.data.core;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.Getter;
import org.springframework.dao.DataAccessException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.r2dbc.core.RowsFetchSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;

/**
 * R2dbcEntityTemplateDecorator 类扩展了 R2dbcEntityTemplate，提供了一种适配器模式，用于自定义 R2DBC 实体操作。
 *
 * @author Panda
 */
@Getter
public class R2dbcEntityTemplateAdapter extends R2dbcEntityTemplate {

    /**
     * 代理对象，用于实际执行数据操作。
     */
    private final ReactiveEntityTemplate delegate;

    /**
     * 构造函数，初始化代理对象并传递数据库客户端和数据访问策略。
     *
     * @param reactiveEntityTemplate 提供数据库客户端和数据访问策略的 ReactiveEntityTemplate 对象。
     */
    public R2dbcEntityTemplateAdapter(ReactiveEntityTemplate reactiveEntityTemplate) {
        super(reactiveEntityTemplate.getDatabaseClient(), reactiveEntityTemplate.getDataAccessStrategy());
        this.delegate = reactiveEntityTemplate;
    }

    /**
     * 计算匹配指定查询条件的实体数量。
     *
     * @param query       查询条件。
     * @param entityClass 实体类型。
     * @return 返回匹配查询条件的实体数量的 Mono 对象。
     * @throws DataAccessException 数据访问异常。
     */
    @Override
    public Mono<Long> count(Query query, Class<?> entityClass) throws DataAccessException {
        return delegate.select(entityClass)
                .matching(query)
                .count();
    }

    /**
     * 检查是否存在匹配指定查询条件的实体。
     *
     * @param query       查询条件。
     * @param entityClass 实体类型。
     * @return 返回是否存在匹配查询条件的实体的 Mono 对象（true/false）。
     * @throws DataAccessException 数据访问异常。
     */
    @Override
    public Mono<Boolean> exists(Query query, Class<?> entityClass) throws DataAccessException {
        return delegate.select(entityClass)
                .matching(query)
                .exists();
    }

    /**
     * 根据查询条件选择实体。
     *
     * @param query       查询条件。
     * @param entityClass 实体类型。
     * @return 返回匹配查询条件的所有实体的 Flux 对象。
     * @throws DataAccessException 数据访问异常。
     */
    @Override
    public <T> Flux<T> select(Query query, Class<T> entityClass) throws DataAccessException {
        return delegate.select(query, entityClass);
    }

    /**
     * 根据查询条件选择一个实体。
     *
     * @param query       查询条件。
     * @param entityClass 实体类型。
     * @return 返回匹配查询条件的第一个实体的 Mono 对象。
     * @throws DataAccessException 数据访问异常。
     */
    @Override
    public <T> Mono<T> selectOne(Query query, Class<T> entityClass) throws DataAccessException {
        return delegate.selectOne(query, entityClass);
    }

    /**
     * 根据查询条件更新实体。
     *
     * @param query       查询条件。
     * @param update      更新操作。
     * @param entityClass 实体类型。
     * @return 返回更新操作影响的行数的 Mono 对象。
     * @throws DataAccessException 数据访问异常。
     */
    @Override
    public Mono<Long> update(Query query, Update update, Class<?> entityClass) throws DataAccessException {
        return delegate.update(query, update, entityClass);
    }

    /**
     * 根据查询条件删除实体。
     *
     * @param query       查询条件。
     * @param entityClass 实体类型。
     * @return 返回删除操作影响的行数的 Mono 对象。
     * @throws DataAccessException 数据访问异常。
     */
    @Override
    public Mono<Long> delete(Query query, Class<?> entityClass) throws DataAccessException {
        return delegate.delete(query, entityClass);
    }

    /**
     * 执行准备好的操作并返回行数据的查询规格。
     *
     * @param operation   准备好的操作。
     * @param entityClass 实体类型。
     * @param <T>         返回类型。
     * @return 返回行数据的查询规格的 RowsFetchSpec 对象。
     * @throws DataAccessException 数据访问异常。
     */
    @Override
    public <T> RowsFetchSpec<T> query(PreparedOperation<?> operation, Class<T> entityClass) throws DataAccessException {
        return delegate.query(operation, entityClass);
    }

    /**
     * 执行准备好的操作并返回行数据的查询规格（使用自定义行映射器）。
     *
     * @param operation 准备好的操作。
     * @param rowMapper 行数据映射器。
     * @param <T>       返回类型。
     * @return 返回行数据的查询规格的 RowsFetchSpec 对象。
     * @throws DataAccessException 数据访问异常。
     */
    @Override
    public <T> RowsFetchSpec<T> query(PreparedOperation<?> operation, BiFunction<Row, RowMetadata, T> rowMapper) throws DataAccessException {
        return delegate.query(operation, rowMapper);
    }

    /**
     * 执行准备好的操作并返回行数据的查询规格（使用自定义行映射器和实体类型）。
     *
     * @param operation   准备好的操作。
     * @param entityClass 实体类型。
     * @param rowMapper   行数据映射器。
     * @param <T>         返回类型。
     * @return 返回行数据的查询规格的 RowsFetchSpec 对象。
     * @throws DataAccessException 数据访问异常。
     */
    @Override
    public <T> RowsFetchSpec<T> query(PreparedOperation<?> operation, Class<?> entityClass, BiFunction<Row, RowMetadata, T> rowMapper) throws DataAccessException {
        return delegate.query(operation, entityClass, rowMapper);
    }

    /**
     * 插入一个实体。
     *
     * @param entity 要插入的实体。
     * @param <T>    实体类型。
     * @return 返回插入操作的 Mono 对象。
     * @throws DataAccessException 数据访问异常。
     */
    @Override
    public <T> Mono<T> insert(T entity) throws DataAccessException {
        return delegate.insert(entity);
    }

    /**
     * 更新一个实体。
     *
     * @param entity 要更新的实体。
     * @param <T>    实体类型。
     * @return 返回更新操作的 Mono 对象。
     * @throws DataAccessException 数据访问异常。
     */
    @Override
    public <T> Mono<T> update(T entity) throws DataAccessException {
        return delegate.update(entity);
    }

    /**
     * 删除一个实体。
     *
     * @param entity 要删除的实体。
     * @param <T>    实体类型。
     * @return 返回删除操作的 Mono 对象。
     * @throws DataAccessException 数据访问异常。
     */
    @Override
    public <T> Mono<T> delete(T entity) throws DataAccessException {
        return delegate.delete(entity);
    }

    /**
     * 根据领域类型创建删除操作。
     *
     * @param domainType 领域类型。
     * @return 返回创建的删除操作的 ReactiveDelete 对象。
     */
    @Override
    public ReactiveDelete delete(Class<?> domainType) {
        return delegate.delete(domainType);
    }

    /**
     * 根据领域类型创建插入操作。
     *
     * @param <T>        领域类型。
     * @param domainType 领域类型。
     * @return 返回创建的插入操作的 ReactiveInsert 对象。
     */
    @Override
    public <T> ReactiveInsert<T> insert(Class<T> domainType) {
        return delegate.insert(domainType);
    }

    /**
     * 根据领域类型创建选择操作。
     *
     * @param <T>        领域类型。
     * @param domainType 领域类型。
     * @return 返回创建的选择操作的 ReactiveSelect 对象。
     */
    @Override
    public <T> ReactiveSelect<T> select(Class<T> domainType) {
        return delegate.select(domainType);
    }

    /**
     * 根据领域类型创建更新操作。
     *
     * @param domainType 领域类型。
     * @return 返回创建的更新操作的 ReactiveUpdate 对象。
     */
    @Override
    public ReactiveUpdate update(Class<?> domainType) {
        return delegate.update(domainType);
    }

    /**
     * 在对象转换前可能调用的钩子方法。
     *
     * @param object 待转换的对象。
     * @param table  表名。
     * @param <T>    对象类型。
     * @return 返回转换后的对象的 Mono 对象。
     */
    @Override
    protected <T> Mono<T> maybeCallBeforeConvert(T object, SqlIdentifier table) {
        return delegate.maybeCallBeforeConvert(object, table);
    }

    /**
     * 在对象转换后可能调用的钩子方法。
     *
     * @param object 转换后的对象。
     * @param table  表名。
     * @param <T>    对象类型。
     * @return 返回转换后的对象的 Mono 对象。
     */
    @Override
    protected <T> Mono<T> maybeCallAfterConvert(T object, SqlIdentifier table) {
        return delegate.maybeCallAfterConvert(object, table);
    }

    /**
     * 在对象保存前可能调用的钩子方法。
     *
     * @param object 待保存的对象。
     * @param row    出站行，包含将要保存的数据。
     * @param table  表名。
     * @param <T>    对象类型。
     * @return 返回保存后的对象的 Mono 对象。
     */
    @Override
    protected <T> Mono<T> maybeCallBeforeSave(T object, OutboundRow row, SqlIdentifier table) {
        return delegate.maybeCallBeforeSave(object, row, table);
    }

    /**
     * 在对象保存后可能调用的钩子方法。
     *
     * @param object 保存后的对象。
     * @param row    出站行，包含已保存的数据。
     * @param table  表名。
     * @param <T>    对象类型。
     * @return 返回保存后的对象的 Mono 对象。
     */
    @Override
    protected <T> Mono<T> maybeCallAfterSave(T object, OutboundRow row, SqlIdentifier table) {
        return delegate.maybeCallAfterSave(object, row, table);
    }

}

