package com.jxpanda.r2dbc.spring.data.core;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.Nullable;

public class R2dbcOperationSupport {


    protected final ReactiveEntityTemplate template;

    protected final R2dbcOperationCoordinator coordinator;

    public R2dbcOperationSupport(ReactiveEntityTemplate template) {
        this.template = template;
        this.coordinator = template.getCoordinator();
    }


    @Getter(AccessLevel.PROTECTED)
    protected static class R2dbcSupport<T, R> {

        /**
         * entityTemplate
         */
        protected final ReactiveEntityTemplate template;

        /**
         * SQL Executor
         */
        protected final R2dbcOperationCoordinator coordinator;

        /**
         * 领域对象类型，通常是实体对象的类型
         */
        protected final Class<T> domainType;

        /**
         * 返回值类型
         */
        protected final Class<R> returnType;

        /**
         * 查询条件对象
         */
        protected final Query query;

        /**
         * 表名
         */
        protected final SqlIdentifier tableName;


        protected R2dbcSupport(ReactiveEntityTemplate template, Class<T> domainType, Class<R> returnType) {
            this(template, domainType, returnType, null, null);
        }


        protected R2dbcSupport(ReactiveEntityTemplate template, Class<T> domainType, Class<R> returnType, @Nullable Query query, @Nullable SqlIdentifier tableName) {
            this.template = template;
            this.coordinator = template.getCoordinator();
            this.domainType = domainType;
            this.returnType = returnType;
            this.query = query == null ? Query.empty() : query;
            this.tableName = tableName == null ? this.coordinator.getTableName(domainType) : tableName;
        }

    }
}


