package com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model;

import com.jxpanda.r2dbc.spring.data.core.kit.QueryKit;
import com.jxpanda.r2dbc.spring.data.core.operation.executor.R2dbcOperationExecutor;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.ReflectionKit;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.query.Update;
import org.springframework.lang.Nullable;

import java.util.*;

@Getter
@Builder
public class R2dbcPluginContext<T, R> {

    private final Class<T> entityType;
    private final Class<R> resultType;
    @Nullable
    private final T entity;
    @Nullable
    private final R result;
    @Nullable
    private final Update update;
    @Nullable
    private final CriteriaDefinition criteria;
    // 缓存插件链上的执行结果
    @Getter(AccessLevel.PRIVATE)
    private final Map<String, R2dbcPluginResponse<T, R>> pluginResponseCache;

    private final Set<String> disabledPlugins;

    private final Class<? extends R2dbcOperationExecutor<T, R>> executorClass;

    private R2dbcPluginContext(Class<T> entityType, Class<R> resultType,
                               @Nullable T entity, @Nullable R result,
                               @Nullable Update update, @Nullable CriteriaDefinition criteria,
                               Map<String, R2dbcPluginResponse<T, R>> pluginResponseCache,
                               Set<String> disabledPlugins,
                               Class<? extends R2dbcOperationExecutor<T, R>> executorClass) {
        this.entityType = entityType;
        this.resultType = resultType;
        this.entity = entity;
        this.result = result;
        this.update = update;
        this.criteria = criteria;
        this.pluginResponseCache = pluginResponseCache == null ? new HashMap<>(8) : pluginResponseCache;
        this.disabledPlugins = disabledPlugins;
        this.executorClass = executorClass;
    }


    public R2dbcPluginContext<T, R> disablePlugin(String pluginName) {
        this.disabledPlugins.add(pluginName);
        return this;
    }

    public R2dbcPluginResponse<T, R> getPluginResponse(String pluginName) {
        return pluginResponseCache.get(pluginName);
    }

    public R2dbcPluginContext<T, R> withEntity(R2dbcOperationExecutor<T, R> executor, T entity) {
        return rebuilder()
                .entity(entity)
                .build();
    }

    public R2dbcPluginContext<T, R> withResult(R2dbcOperationExecutor<T, R> executor, R result) {
        return rebuilder()
                .result(result)
                .build();
    }

    public R2dbcPluginContext<T, R> withCriteria(R2dbcOperationExecutor<T, R> executor, CriteriaDefinition criteriaDefinition) {
        return rebuilder()
                .criteria(QueryKit.mergeCriteria(this.getCriteria(), criteriaDefinition))
                .build();
    }

    public R2dbcPluginContext<T, R> withUpdate(R2dbcOperationExecutor<T, R> executor, Update update) {
        return rebuilder()
                .update(QueryKit.meergeUpdate(this.getUpdate(), update))
                .build();
    }

    public boolean isPluginExecuted(String pluginName) {
        return pluginResponseCache.get(pluginName) != null;
    }

    public R2dbcPluginContext<T, R> withPluginResponse(String pluginName, R2dbcPluginResponse<T, R> pluginValue) {
        pluginResponseCache.put(pluginName, pluginValue);
        return rebuilder()
                .entity(pluginValue.getEntity())
                .update(pluginValue.getUpdate())
                .criteria(pluginValue.getCriteria())
                .result(pluginValue.getResult())
                .build();
    }

    public R2dbcPluginContext.R2dbcPluginContextBuilder<T, R> rebuilder() {
        return rebuilder(null);
    }

    public R2dbcPluginContext.R2dbcPluginContextBuilder<T, R> rebuilder(R2dbcOperationExecutor<T, R> executor) {
        return R2dbcPluginContext.<T, R>builder()
                .entityType(entityType)
                .resultType(resultType)
                .entity(entity)
                .result(result)
                .update(update)
                .criteria(criteria)
                .pluginResponseCache(pluginResponseCache)
                .disabledPlugins(disabledPlugins)
                .executorClass(executor == null ? executorClass : ReflectionKit.cast(executor.getClass()));
    }

}
