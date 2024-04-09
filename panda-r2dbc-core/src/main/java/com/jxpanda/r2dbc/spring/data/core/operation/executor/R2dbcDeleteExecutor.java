package com.jxpanda.r2dbc.spring.data.core.operation.executor;

import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2DbcLogicDeletePlugin;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcPluginContext;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcPluginName;
import com.jxpanda.r2dbc.spring.data.core.kit.QueryKit;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.util.Pair;
import org.springframework.r2dbc.core.PreparedOperation;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.function.Function;

public class R2dbcDeleteExecutor<T, R> extends R2dbcOperationExecutor.WriteExecutor<T, R> {


    private R2dbcDeleteExecutor(R2dbcOperationParameter<T, R> operationParameter, Function<R2dbcOperationParameter<T, R>, Query> queryHandler) {
        super(operationParameter, queryHandler);
    }

    public static <T, R> R2dbcDeleteExecutorBuilder<T, R> builder() {
        return new R2dbcDeleteExecutorBuilder<>();
    }

    @Override
    protected Mono<R> fetch(R2dbcOperationParameter<T, R> parameter) {

        // 尝试执行逻辑删除插件
        R2dbcPluginContext<T, R, Update> pluginContext = parameter.createPluginContext(R2dbcPluginName.LOGIC_DELETE, Update.class);
        Optional<Update> logicDeleteUpdate = pluginExecutor().run(pluginContext).takeResult();
        // 如果插件执行成功的话，则直接执行更新操作
        if (logicDeleteUpdate.isPresent()) {
            return swap(R2dbcUpdateExecutor::builder)
                    .updateSupplier(() -> {
                        Pair<String, Object> logicDeleteColumn = R2DbcLogicDeletePlugin.getLogicDeleteColumn(parameter.getDomainType(), R2DbcLogicDeletePlugin.LogicDeleteValue.DELETE_VALUE);
                        return Update.update(logicDeleteColumn.getFirst(), logicDeleteColumn.getSecond());
                    })
                    .build()
                    .fetch(parameter);
        }

        StatementMapper statementMapper = parameter.getStatementMapper();
        SqlIdentifier tableName = parameter.getTableName();
        Query query = parameter.getQuery();

        StatementMapper.DeleteSpec deleteSpec = statementMapper.createDelete(tableName);

        Optional<CriteriaDefinition> criteria = query.getCriteria();
        if (criteria.isPresent()) {
            deleteSpec = criteria.map(deleteSpec::withCriteria).orElse(deleteSpec);
        }

        PreparedOperation<?> operation = statementMapper.getMappedObject(deleteSpec);
        return this.databaseClient().sql(operation).fetch().rowsUpdated().defaultIfEmpty(0L)
                .cast(parameter.getReturnType());
    }

    @Override
    protected Mono<R> fetch(T domainEntity, R2dbcOperationParameter<T, R> parameter) {
        R2dbcOperationParameter<T, R> newParameter = parameter.rebuild()
                .query(getByIdQuery(domainEntity, parameter.getRelationalPersistentEntity()))
                .build();
        return fetch(newParameter);
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
            return new R2dbcDeleteExecutor<>(operationParameter, queryHandler);
        }

        @Override
        protected R2dbcDeleteExecutorBuilder<T, R> self() {
            return this;
        }
    }

}
