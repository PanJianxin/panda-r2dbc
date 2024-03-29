package com.jxpanda.r2dbc.spring.data.core;

import com.jxpanda.r2dbc.spring.data.core.enhance.key.IdGenerator;
import com.jxpanda.r2dbc.spring.data.core.kit.R2dbcMappingKit;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.r2dbc.support.ArrayUtils;
import org.springframework.data.relational.core.dialect.ArrayColumns;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.Collection;

/**
 * @author Panda
 */
public class R2dbcOperationSupport {


    protected final ReactiveEntityTemplate template;


    public R2dbcOperationSupport(ReactiveEntityTemplate template) {
        this.template = template;
    }


    @SuppressWarnings("deprecation")
    @Getter(AccessLevel.PROTECTED)
    protected static class R2dbcSupport<T> {

        /**
         * entityTemplate
         */
        protected final ReactiveEntityTemplate reactiveEntityTemplate;

        /**
         * 领域对象类型，通常是实体对象的类型
         */
        protected final Class<T> domainType;


        /**
         * 查询条件对象
         */
        protected final Query query;

        /**
         * 表名
         */
        protected final SqlIdentifier tableName;


        protected R2dbcSupport(ReactiveEntityTemplate reactiveEntityTemplate, Class<T> domainType) {
            this(reactiveEntityTemplate, domainType, null, null);
        }


        protected R2dbcSupport(ReactiveEntityTemplate reactiveEntityTemplate, Class<T> domainType, @Nullable Query query, @Nullable SqlIdentifier tableName) {
            this.reactiveEntityTemplate = reactiveEntityTemplate;
            this.domainType = domainType;
            this.query = query == null ? Query.empty() : query;
            this.tableName = tableName == null ? R2dbcMappingKit.getTableName(domainType) : tableName;
        }

        protected SpelAwareProxyProjectionFactory projectionFactory() {
            return this.reactiveEntityTemplate.getProjectionFactory();
        }

        protected StatementMapper statementMapper() {
            return this.reactiveEntityTemplate.getStatementMapper();
        }

        protected R2dbcConverter converter() {
            return this.reactiveEntityTemplate.getConverter();
        }

        protected DatabaseClient databaseClient() {
            return this.reactiveEntityTemplate.getDatabaseClient();
        }

        protected IdGenerator<?> idGenerator() {
            return this.reactiveEntityTemplate.getIdGenerator();
        }

        protected Dialect dialect() {
            return this.reactiveEntityTemplate.getDialect();
        }

        protected TransactionalOperator transactionalOperator() {
            return this.reactiveEntityTemplate.getTransactionalOperator();
        }

        protected TransactionalOperator transactionalOperator(int propagationBehavior, int isolationLevel, int timeout, boolean readOnly) {
            return TransactionalOperator.create(this.reactiveEntityTemplate.getR2dbcTransactionManager(), new TransactionDefinition() {
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


        @SuppressWarnings("deprecation")
        protected OutboundRow getOutboundRow(Object object) {

            Assert.notNull(object, "Entity object must not be null");

            OutboundRow row = new OutboundRow();

            this.converter().write(object, row);

            RelationalPersistentEntity<?> entity = R2dbcMappingKit.getRequiredEntity(ClassUtils.getUserClass(object));

            for (RelationalPersistentProperty property : entity) {

                Parameter value = row.get(property.getColumnName());
                if (shouldConvertArrayValue(property, value)) {
                    Parameter writeValue = getArrayValue(value, property);
                    row.put(property.getColumnName(), writeValue);
                }
            }

            return row;
        }

        private boolean shouldConvertArrayValue(RelationalPersistentProperty property, Parameter value) {

            if (!property.isCollectionLike()) {
                return false;
            }

            // noinspection AlibabaAvoidComplexCondition
            if (value.getValue() != null && (value.getValue() instanceof Collection || value.getValue().getClass().isArray())) {
                return true;
            }

            return Collection.class.isAssignableFrom(value.getType()) || value.getType().isArray();
        }

        private Parameter getArrayValue(Parameter value, RelationalPersistentProperty property) {

            if (value.getValue() == null || value.getType().equals(byte[].class)) {
                return value;
            }

            ArrayColumns arrayColumns = this.dialect().getArraySupport();

            if (!arrayColumns.isSupported()) {
                throw new InvalidDataAccessResourceUsageException(
                        "Dialect " + this.dialect().getClass().getName() + " does not support array columns");
            }

            Class<?> actualType = null;
            if (value.getValue() instanceof Collection) {
                actualType = CollectionUtils.findCommonElementType((Collection<?>) value.getValue());
            } else if (!value.isEmpty() && value.getValue().getClass().isArray()) {
                actualType = value.getValue().getClass().getComponentType();
            }

            if (actualType == null) {
                actualType = property.getActualType();
            }

            actualType = this.converter().getTargetType(actualType);

            if (value.isEmpty()) {

                Class<?> targetType = arrayColumns.getArrayType(actualType);
                int depth = actualType.isArray() ? ArrayUtils.getDimensionDepth(actualType) : 1;
                Class<?> targetArrayType = ArrayUtils.getArrayClass(targetType, depth);
                return Parameter.empty(targetArrayType);
            }

            return Parameter.fromOrEmpty(this.converter().getArrayValue(arrayColumns, property, value.getValue()),
                    actualType);
        }

    }

}


