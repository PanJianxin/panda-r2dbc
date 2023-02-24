package com.jxpanda.r2dbc.spring.data.core;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.springframework.dao.DataAccessException;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.r2dbc.core.RowsFetchSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;

public class R2dbcEntityTemplateAdapter extends R2dbcEntityTemplate {


    private final ReactiveEntityTemplate delegate;

    public R2dbcEntityTemplateAdapter(ReactiveEntityTemplate reactiveEntityTemplate) {
        super(reactiveEntityTemplate.getDatabaseClient(), reactiveEntityTemplate.getDataAccessStrategy());
        this.delegate = reactiveEntityTemplate;
    }

    @Override
    public DatabaseClient getDatabaseClient() {
        return delegate.getDatabaseClient();
    }

    @Override
    @SuppressWarnings("deprecation")
    public ReactiveDataAccessStrategy getDataAccessStrategy() {
        return delegate.getDataAccessStrategy();
    }

    @Override
    public R2dbcConverter getConverter() {
        return delegate.getConverter();
    }

    @Override
    public Mono<Long> count(Query query, Class<?> entityClass) throws DataAccessException {
        return delegate.select(entityClass)
                .matching(query)
                .count();
    }

    @Override
    public Mono<Boolean> exists(Query query, Class<?> entityClass) throws DataAccessException {
        return delegate.select(entityClass)
                .matching(query)
                .exists();
    }

    @Override
    public <T> Flux<T> select(Query query, Class<T> entityClass) throws DataAccessException {
        return delegate.select(query, entityClass);
    }

    @Override
    public <T> Mono<T> selectOne(Query query, Class<T> entityClass) throws DataAccessException {
        return delegate.selectOne(query, entityClass);
    }

    @Override
    public Mono<Long> update(Query query, Update update, Class<?> entityClass) throws DataAccessException {
        return delegate.update(query, update, entityClass);
    }

    @Override
    public Mono<Long> delete(Query query, Class<?> entityClass) throws DataAccessException {
        return delegate.delete(query, entityClass);
    }

    @Override
    public <T> RowsFetchSpec<T> query(PreparedOperation<?> operation, Class<T> entityClass) throws DataAccessException {
        return delegate.query(operation, entityClass);
    }


    @Override
    public <T> RowsFetchSpec<T> query(PreparedOperation<?> operation, BiFunction<Row, RowMetadata, T> rowMapper) throws DataAccessException {
        return delegate.query(operation, rowMapper);
    }


    @Override
    public <T> RowsFetchSpec<T> query(PreparedOperation<?> operation, Class<?> entityClass, BiFunction<Row, RowMetadata, T> rowMapper) throws DataAccessException {
        return delegate.query(operation, entityClass, rowMapper);
    }

    @Override
    public <T> Mono<T> insert(T entity) throws DataAccessException {
        return delegate.insert(entity);
    }

    @Override
    public <T> Mono<T> update(T entity) throws DataAccessException {
        return delegate.update(entity);
    }

    @Override
    public <T> Mono<T> delete(T entity) throws DataAccessException {
        return delegate.delete(entity);
    }

    @Override
    public ReactiveDelete delete(Class<?> domainType) {
        return delegate.delete(domainType);
    }

    @Override
    public <T> ReactiveInsert<T> insert(Class<T> domainType) {
        return delegate.insert(domainType);
    }

    @Override
    public <T> ReactiveSelect<T> select(Class<T> domainType) {
        return delegate.select(domainType);
    }

    @Override
    public ReactiveUpdate update(Class<?> domainType) {
        return delegate.update(domainType);
    }

    @Override
    protected <T> Mono<T> maybeCallBeforeConvert(T object, SqlIdentifier table) {
        return delegate.maybeCallBeforeConvert(object, table);
    }

    @Override
    protected <T> Mono<T> maybeCallAfterConvert(T object, SqlIdentifier table) {
        return delegate.maybeCallAfterConvert(object, table);
    }

    @Override
    protected <T> Mono<T> maybeCallBeforeSave(T object, OutboundRow row, SqlIdentifier table) {
        return delegate.maybeCallBeforeSave(object, row, table);
    }

    @Override
    protected <T> Mono<T> maybeCallAfterSave(T object, OutboundRow row, SqlIdentifier table) {
        return delegate.maybeCallAfterSave(object, row, table);
    }

}
