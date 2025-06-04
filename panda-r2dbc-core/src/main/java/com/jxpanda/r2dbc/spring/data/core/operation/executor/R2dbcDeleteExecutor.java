package com.jxpanda.r2dbc.spring.data.core.operation.executor;

import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model.R2dbcPluginContext;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model.R2dbcPluginEnum;
import com.jxpanda.r2dbc.spring.data.core.kit.QueryKit;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.r2dbc.core.PreparedOperation;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class R2dbcDeleteExecutor<T, R> extends R2dbcOperationExecutor.WriteExecutor<T, R> {


    private R2dbcDeleteExecutor(R2dbcOperationContext<T, R> operationContext, Function<R2dbcOperationContext<T, R>, Query> queryHandler) {
        super(operationContext, queryHandler);
    }

    public static <T, R> R2dbcDeleteExecutorBuilder<T, R> builder() {
        return new R2dbcDeleteExecutorBuilder<>();
    }

    private Mono<R2dbcPluginContext<T, R>> fetchBefore(R2dbcOperationContext<T, R> operationContext) {
        R2dbcOperationContext<T, R> context = operationContext;
        if (operationContext.getEntity() != null) {
            context = operationContext.rebuilder()
                    .query(getByIdQuery(operationContext.getEntity(), Objects.requireNonNull(operationContext.getRelationalPersistentEntity())))
                    .build();
        }
        return pluginExecutor().runBefore(context.createPluginContext(this));
    }

    private Mono<R> fetchAfter(R2dbcOperationContext<T, R> operationContext, R2dbcPluginContext<T, R> pluginContext) {
        return pluginExecutor().runAfter(pluginContext)
                .mapNotNull(R2dbcPluginContext::getResult)
                .doOnSuccess(operationContext::withResult);

    }

    @Override
    protected Mono<R> fetch(R2dbcOperationContext<T, R> operationContext) {
        return fetchBefore(operationContext)
                .flatMap(pluginContext -> {
                    Mono<R> fetchMono;
                    if (pluginContext.isPluginExecuted(R2dbcPluginEnum.Name.LOGIC_DELETE.name()) && pluginContext.getUpdate() != null) {
                        fetchMono = swap(R2dbcUpdateExecutor::builder)
                                .updateSupplier(pluginContext::getUpdate)
                                .build()
                                .fetch(operationContext);
                    } else {
                        StatementMapper statementMapper = operationContext.getStatementMapper();
                        SqlIdentifier tableName = operationContext.getTableName();
                        Query query = operationContext.getQuery();
                        StatementMapper.DeleteSpec deleteSpec = statementMapper.createDelete(tableName);
                        Optional<CriteriaDefinition> criteria = query.getCriteria();
                        if (criteria.isPresent()) {
                            deleteSpec = criteria.map(deleteSpec::withCriteria).orElse(deleteSpec);
                        }
                        PreparedOperation<?> operation = statementMapper.getMappedObject(deleteSpec);
                        fetchMono = this.databaseClient()
                                .sql(operation)
                                .filter(template().getStatementFilterFunction())
                                .fetch()
                                .rowsUpdated()
                                .defaultIfEmpty(0L)
                                .cast(operationContext.getResultType());
                    }
                    return fetchMono.map(result -> pluginContext.withResult(this, result));
                })
                .flatMap(pluginContext -> fetchAfter(operationContext, pluginContext));

    }

    private <E> Query getByIdQuery(E entity, RelationalPersistentEntity<E> persistentEntity) {

        if (!persistentEntity.hasIdProperty()) {
            throw new MappingException("No id property found for object of type " + persistentEntity.getType());
        }

        IdentifierAccessor identifierAccessor = persistentEntity.getIdentifierAccessor(entity);
        Object id = identifierAccessor.getRequiredIdentifier();

        return QueryKit.queryById(persistentEntity.getType(), id);
    }


    public static final class R2dbcDeleteExecutorBuilder<T, R> extends R2dbcOperationExecutor.R2dbcExecutorBuilder<T, R, R2dbcDeleteExecutor<T, R>, R2dbcDeleteExecutorBuilder<T, R>> {

        public R2dbcDeleteExecutor<T, R> buildExecutor() {
            return new R2dbcDeleteExecutor<>(operationContext, queryHandler);
        }

        @Override
        protected R2dbcDeleteExecutorBuilder<T, R> self() {
            return this;
        }
    }

}
