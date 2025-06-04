package com.jxpanda.r2dbc.spring.data.core.operation.executor;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.core.enhance.key.IdGenerator;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcPluginExecutor;
import com.jxpanda.r2dbc.spring.data.core.kit.R2dbcMappingKit;
import io.r2dbc.spi.Statement;
import lombok.AccessLevel;
import lombok.Getter;
import org.reactivestreams.Publisher;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.r2dbc.support.ArrayUtils;
import org.springframework.data.relational.core.dialect.ArrayColumns;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Query;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.r2dbc.core.RowsFetchSpec;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Panda
 */
@Getter(AccessLevel.PROTECTED)
@SuppressWarnings({"AlibabaAbstractClassShouldStartWithAbstractNaming", "deprecation"})
public class R2dbcOperationExecutor<T, R> {

    private final R2dbcOperationContext<T, R> operationContext;

    private final Function<R2dbcOperationContext<T, R>, Query> queryHandler;

    public R2dbcOperationExecutor(R2dbcOperationContext<T, R> operationContext, Function<R2dbcOperationContext<T, R>, Query> queryHandler) {
        this.operationContext = operationContext;
        this.queryHandler = queryHandler != null ? queryHandler : R2dbcOperationContext::getQuery;
    }

    protected R2dbcOperationExecutor<T, R> handleQuery() {
        R2dbcOperationContext<T, R> operationContext = this.operationContext;
        Query query = queryHandler.apply(operationContext);
        if (query != operationContext.getQuery()) {
            operationContext = operationContext.rebuilder().query(query).build();
        }
        return new R2dbcOperationExecutor<>(operationContext, queryHandler);
    }


    protected <E extends R2dbcOperationExecutor<T, R>, B extends R2dbcOperationExecutor.R2dbcExecutorBuilder<T, R, E, B>> B swap(Supplier<B> builderSupplier) {
        return builderSupplier.get()
                .operationContext(operationContext)
                .queryHandler(queryHandler);
    }


    protected ReactiveEntityTemplate template() {
        return this.operationContext.getTemplate();
    }

    protected R2dbcPluginExecutor pluginExecutor() {
        return template().getPluginExecutor();
    }

    @SuppressWarnings("deprecation")
    protected ReactiveDataAccessStrategy dataAccessStrategy() {
        return template().getDataAccessStrategy();
    }

    protected SpelAwareProxyProjectionFactory projectionFactory() {
        return template().getProjectionFactory();
    }

    protected StatementMapper statementMapper() {
        return template().getStatementMapper();
    }

    protected R2dbcConverter converter() {
        return template().getConverter();
    }

    protected DatabaseClient databaseClient() {
        return template().getDatabaseClient();
    }

    protected IdGenerator<?> idGenerator() {
        return template().getIdGenerator();
    }

    protected Dialect dialect() {
        return template().getDialect();
    }

    protected Function<Statement, Statement> statementFilterFunction() {
        return template().getStatementFilterFunction();
    }

    protected TransactionalOperator transactionalOperator() {
        return template().getTransactionalOperator();
    }

    protected TransactionalOperator transactionalOperator(int propagationBehavior, int isolationLevel, int timeout, boolean readOnly) {
        return TransactionalOperator.create(template().getR2dbcTransactionManager(), new TransactionDefinition() {
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


    //---------------------------------------------------------------------------------
    // 以下几个函数在insert和update中用到
    //---------------------------------------------------------------------------------
    protected OutboundRow getOutboundRow(Object object) {

        Assert.notNull(object, "Entity object must not be null");

        OutboundRow row = new OutboundRow();

        converter().write(object, row);

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

    protected static abstract class ReadExecutor<T, R> extends R2dbcOperationExecutor<T, R> {


        public ReadExecutor(R2dbcOperationContext<T, R> operationContext, Function<R2dbcOperationContext<T, R>, Query> queryHandler) {
            super(operationContext, queryHandler);
        }

        public <P extends Publisher<R>> P execute(Function<RowsFetchSpec<R>, P> resultHandler) {
            return fetch(handleQuery().getOperationContext(), resultHandler);
        }

        protected abstract <P extends Publisher<R>> P fetch(R2dbcOperationContext<T, R> operationContext, Function<RowsFetchSpec<R>, P> resultHandler);

    }

    protected static abstract class WriteExecutor<T, R> extends R2dbcOperationExecutor<T, R> {


        public WriteExecutor(R2dbcOperationContext<T, R> operationContext, Function<R2dbcOperationContext<T, R>, Query> queryHandler) {
            super(operationContext, queryHandler);
        }

//        protected abstract Mono<R> fetch(T entity, R2dbcOperationContext<T, R> operationContext);

        protected abstract Mono<R> fetch(R2dbcOperationContext<T, R> operationContext);


        public Mono<R> execute() {
            return fetch(handleQuery().getOperationContext());
        }

        public Mono<R> execute(T entity) {
            return fetch(handleQuery()
                    .getOperationContext()
                    .rebuilder()
                    .entity(Objects.requireNonNull(entity))
                    .build());
        }

        /**
         * 批量执行
         * 暂时使用循环来做
         * 后期考虑通过批量相关的语句来做
         */
        public Flux<R> executeBatch(Collection<T> entityList) {
            if (ObjectUtils.isEmpty(entityList)) {
                return Flux.empty();
            }
            // TODO：
            //  这里是使用循环来一条条执行的，需要进一步研究一下：
            //  1、是否能进一步优化多线程的处理？
            //  2、能否把SQL语句转成批量处理模式
            return Flux.fromIterable(entityList)
                    .flatMap(this::execute)
                    .switchIfEmpty(Flux.empty())
                    .as(transactionalOperator()::transactional);
        }


    }


    @SuppressWarnings("AlibabaAbstractClassShouldStartWithAbstractNaming")
    public static abstract class R2dbcExecutorBuilder<T, R, E extends R2dbcOperationExecutor<T, R>, B extends R2dbcExecutorBuilder<T, R, E, B>> {
        protected R2dbcOperationContext<T, R> operationContext;

        protected Function<R2dbcOperationContext<T, R>, Query> queryHandler;

        private Class<T> entityType;

        private Class<R> resultType;

        public final E build() {
            if (entityType != null || resultType != null) {
                operationContext = operationContext.rebuilder()
                        .entityType(entityType == null ? operationContext.getEntityType() : entityType)
                        .resultType(resultType == null ? operationContext.getResultType() : resultType)
                        .build();
            }
            return buildExecutor();
        }

        /**
         * 返回builder对象自身
         *
         * @return builder对象自身
         */
        protected abstract B self();

        /**
         * 构建SQL执行器
         *
         * @return SQL执行器
         */
        protected abstract E buildExecutor();

        public B operationContext(R2dbcOperationContext<T, R> operationContext) {
            this.operationContext = operationContext;
            return self();
        }

        public B queryHandler(Function<R2dbcOperationContext<T, R>, Query> queryHandler) {
            this.queryHandler = queryHandler;
            return self();
        }

        public B entityType(Class<T> entityType) {
            this.entityType = entityType;
            return self();
        }

        public B resultType(Class<R> resultType) {
            this.resultType = resultType;
            return self();
        }

    }

}
