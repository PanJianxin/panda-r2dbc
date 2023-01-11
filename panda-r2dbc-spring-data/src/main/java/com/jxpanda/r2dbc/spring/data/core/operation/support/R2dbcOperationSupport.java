package com.jxpanda.r2dbc.spring.data.core.operation.support;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.Nullable;

public class R2dbcOperationSupport {


    protected final ReactiveEntityTemplate template;

    protected final R2dbcSQLExecutor sqlExecutor;

    public R2dbcOperationSupport(ReactiveEntityTemplate template) {
        this.template = template;
        this.sqlExecutor = template.getSqlExecutor();
    }



    protected static class R2dbcSupport<T, R> {

        /**
         * entityTemplate
         */
        private final ReactiveEntityTemplate template;

        /**
         * SQL Executor
         * */
        private final R2dbcSQLExecutor executor;

        /**
         * 领域对象类型，通常是实体对象的类型
         */
        private final Class<T> domainType;

        /**
         * 返回值类型
         */
        private final Class<R> returnType;

        /**
         * 查询条件对象
         */
        private final Query query;

        /**
         * 表名
         */
        private final SqlIdentifier tableName;


        protected R2dbcSupport(ReactiveEntityTemplate template, Class<T> domainType, Class<R> returnType) {
            this(template, domainType, returnType, null, null);
        }


        protected R2dbcSupport(ReactiveEntityTemplate template, Class<T> domainType, Class<R> returnType, @Nullable Query query, @Nullable SqlIdentifier tableName) {
            this.template = template;
            this.executor = template.getSqlExecutor();
            this.domainType = domainType;
            this.returnType = returnType;
            this.query = query == null ? Query.empty() : query;
            this.tableName = tableName == null ? this.executor.getTableName(domainType) : tableName;
        }

        protected ReactiveEntityTemplate getTemplate() {
            return template;
        }

        protected R2dbcSQLExecutor getExecutor() {
            return executor;
        }

        protected Class<T> getDomainType() {
            return domainType;
        }

        protected Class<R> getReturnType() {
            return returnType;
        }

        protected Query getQuery() {
            return query;
        }

        protected SqlIdentifier getTableName() {
            return tableName;
        }

    }
}


