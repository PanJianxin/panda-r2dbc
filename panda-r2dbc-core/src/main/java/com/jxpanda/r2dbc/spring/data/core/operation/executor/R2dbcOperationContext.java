package com.jxpanda.r2dbc.spring.data.core.operation.executor;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableEntity;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model.R2dbcPluginContext;
import com.jxpanda.r2dbc.spring.data.core.kit.R2dbcMappingKit;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.ReflectionKit;
import io.r2dbc.spi.Statement;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.lang.Nullable;

import java.util.Optional;
import java.util.function.Function;

/**
 * @author Panda
 */

@Getter
@Builder
public final class R2dbcOperationContext<T, R> {

    /**
     * template
     */
    private final ReactiveEntityTemplate template;
    private final Update update;
    private final Query query;
    private final T entity;
    private final R result;
    private final Class<T> entityType;
    private final Class<R> resultType;
    private final SqlIdentifier tableName;
    private final OutboundRow outboundRow;

    /**
     * 这个是自定义参数，支持通过修改这个参数来覆盖某些设定好的值
     * 例如：临时禁用逻辑删除插件
     */
    private final R2dbcOperationOption option;

    /**
     * 新增的过滤函数
     *
     * @since spring data r2dbc 3.4.0
     */
    private final Function<? super Statement, ? extends Statement> filterFunction;

    // -------------------------------------------------------------------------
    // 以下是计算属性
    // -------------------------------------------------------------------------

    /**
     * relationalPersistentEntity
     */
    private final RelationalPersistentEntity<T> relationalPersistentEntity;
    /**
     * statementMapper
     */
    private final StatementMapper statementMapper;

    private final boolean simpleResultType;

    R2dbcOperationContext(ReactiveEntityTemplate template, Update update, Query query, T entity, R result, Class<T> entityType, Class<R> resultType, SqlIdentifier tableName, OutboundRow outboundRow, R2dbcOperationOption option, Function<? super Statement, ? extends Statement> filterFunction) {
        this.template = template;
        this.update = update;
        this.query = query == null ? Query.empty() : query;
        this.entity = entity;
        this.result = result;
        this.entityType = entityType;
        this.resultType = resultType;
        this.tableName = tableName == null ? R2dbcMappingKit.getTableName(entityType) : tableName;
        this.outboundRow = outboundRow;
        this.option = option == null ? new R2dbcOperationOption() : option;
        this.filterFunction = filterFunction == null ? Function.identity() : filterFunction;

        // 以下是计算属性的初始化
        this.relationalPersistentEntity = R2dbcMappingKit.getPersistentEntity(entityType);
        this.statementMapper = buildStatementMapper();
        this.simpleResultType = this.resultType != null && template.getConverter().isSimpleType(this.resultType);
    }

    /**
     * 这个是全参数构造器，为了满足@Builder生成的Builder类所使用的
     * 之所以要重写的原因是其中relationalPersistentEntity，statementMapper，simpleResultType是计算属性，禁止覆盖的，要刻意丢弃
     */
    public R2dbcOperationContext(ReactiveEntityTemplate template,
                                 Update update,
                                 Query query,
                                 T entity,
                                 R result,
                                 Class<T> entityType,
                                 Class<R> resultType,
                                 SqlIdentifier tableName,
                                 OutboundRow outboundRow,
                                 R2dbcOperationOption option,
                                 Function<? super Statement, ? extends Statement> filterFunction,
                                 RelationalPersistentEntity<T> relationalPersistentEntity,
                                 StatementMapper statementMapper,
                                 boolean simpleResultType) {
        this(template, update, query, entity, result, entityType, resultType, tableName, outboundRow, option, filterFunction);
    }

    @SuppressWarnings("UnusedReturnValue")
    public R2dbcOperationContext<T, R> withResult(R result) {
        return rebuilder().result(result).build();
    }

    public R2dbcOperationContextBuilder<T, R> rebuilder() {
        return rebuilder(entityType, resultType);
    }

    public <NT, NR> R2dbcOperationContextBuilder<NT, NR> rebuilder(Class<NT> entityType, Class<NR> resultType) {
        return R2dbcOperationContext.<NT, NR>builder()
                .template(template)
                .option(option)
                .query(query)
                .entityType(entityType)
                .tableName(tableName)
                .resultType(resultType)
                .outboundRow(outboundRow)
                .filterFunction(filterFunction);
    }

    public R2dbcPluginContext<T, R> createPluginContext(R2dbcOperationExecutor<T, R> executor) {
        return createPluginContext(executor, null);
    }

    public R2dbcPluginContext<T, R> createPluginContext(R2dbcOperationExecutor<T, R> executor, @Nullable T entity) {
        return R2dbcPluginContext.<T, R>builder()
                .entity(entity == null ? this.entity : entity)
                .result(result)
                .entityType(entityType)
                .resultType(resultType)
                .update(update)
                .criteria(query.getCriteria().orElse(null))
                .disabledPlugins(this.option.getDisabledPlugins())
                .executorClass(ReflectionKit.cast(executor.getClass()))
                .build();
    }

    private StatementMapper buildStatementMapper() {
        // 是否是聚合对象
        boolean isAggregate = Optional.ofNullable(entityType.getAnnotation(TableEntity.class))
                .map(TableEntity::aggregate)
                .orElse(false);
        return isAggregate ? template.getStatementMapper() : template.getStatementMapper().forType(entityType);
    }


}
