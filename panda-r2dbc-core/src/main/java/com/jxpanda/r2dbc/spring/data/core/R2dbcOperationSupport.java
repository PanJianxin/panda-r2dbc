package com.jxpanda.r2dbc.spring.data.core;

import com.jxpanda.r2dbc.spring.data.config.R2dbcConfigProperties;
import com.jxpanda.r2dbc.spring.data.core.enhance.key.IdGenerator;
import com.jxpanda.r2dbc.spring.data.core.kit.MappingKit;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionalOperator;

public class R2dbcOperationSupport {


    protected final ReactiveEntityTemplate template;


    public R2dbcOperationSupport(ReactiveEntityTemplate template) {
        this.template = template;
    }


    @Getter(AccessLevel.PROTECTED)
    protected static class R2dbcSupport<T, R> {

        /**
         * entityTemplate
         */
        protected final ReactiveEntityTemplate template;

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
            this.domainType = domainType;
            this.returnType = returnType;
            this.query = query == null ? Query.empty() : query;
            this.tableName = tableName == null ? MappingKit.getTableName(domainType) : tableName;
        }

        R2dbcConfigProperties r2dbcConfigProperties() {
            return this.template.getR2dbcConfigProperties();
        }

        SpelAwareProxyProjectionFactory projectionFactory() {
            return this.template.getProjectionFactory();
        }

        MappingContext<? extends RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> mappingContext() {
            return this.template.getConverter().getMappingContext();
        }

        StatementMapper statementMapper() {
            return this.template.getDataAccessStrategy().getStatementMapper();
        }

        R2dbcConverter converter() {
            return this.template.getConverter();
        }

        DatabaseClient databaseClient() {
            return this.template.getDatabaseClient();
        }

        IdGenerator<?> idGenerator() {
            return this.template.getIdGenerator();
        }

        TransactionalOperator transactionalOperator() {
            return this.template.getTransactionalOperator();
        }

        TransactionalOperator transactionalOperator(int propagationBehavior, int isolationLevel, int timeout, boolean readOnly) {
            return TransactionalOperator.create(this.template.getR2dbcTransactionManager(), new TransactionDefinition() {
                @Override
                public int getPropagationBehavior() {
                    return propagationBehavior;
                }

                @Override
                public int getIsolationLevel() {
                    return isolationLevel;
                }

                @Override
                public int getTimeout() {
                    return timeout;
                }

                @Override
                public boolean isReadOnly() {
                    return readOnly;
                }
            });
        }

    }
}


