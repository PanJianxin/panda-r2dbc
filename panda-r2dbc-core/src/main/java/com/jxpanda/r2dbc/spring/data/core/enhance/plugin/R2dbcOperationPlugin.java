package com.jxpanda.r2dbc.spring.data.core.enhance.plugin;


import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.contract.R2dbcCriteriaPlugin;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.contract.R2dbcEntityPlugin;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.contract.R2dbcResultPlugin;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.contract.R2dbcUpdatePlugin;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model.R2dbcPluginContext;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model.R2dbcPluginEnum;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model.R2dbcPluginResponse;
import com.jxpanda.r2dbc.spring.data.core.operation.executor.R2dbcUpdateExecutor;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.ReflectionKit;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.query.Update;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple4;

import java.util.Optional;

@Getter
public abstract class R2dbcOperationPlugin {


    private final String pluginName;

    @Setter
    private Integer sort;

    public R2dbcOperationPlugin(R2dbcPluginEnum.Name pluginName) {
        this(pluginName.name(), pluginName.getDefaultSort());
    }

    public R2dbcOperationPlugin(String pluginName) {
        this(pluginName, 0);
    }

    public R2dbcOperationPlugin(String pluginName, Integer sort) {
        this.pluginName = pluginName;
        this.sort = sort;
    }

    private boolean isDisable(R2dbcPluginContext<?, ?> context) {
        return context.getDisabledPlugins().contains(pluginName);
    }

    /**
     * 核心分发逻辑：遍历 Phase 枚举，让匹配阶段调用 handleXxx(...)，并收集非空的新值，
     * 最后一次性创建一个新的 R2dbcPluginContext 返回。
     */
    public <T, R> Mono<R2dbcPluginContext<T, R>> apply(R2dbcPluginContext<T, R> context) {
        if (isDisable(context)) {
            return Mono.just(context);
        }
        return Mono.zip(handleEntityIfMatch(context), handleUpdateIfMatch(context), handleCriteriaIfMatch(context), handleResultIfMatch(context))
                .subscribeOn(Schedulers.boundedElastic())
                .map(tuple -> buildPluginResponse(tuple, context))
                .map(pluginResponse -> context.withPluginResponse(this.getPluginName(), pluginResponse));
    }

    private <T, R> R2dbcPluginResponse<T, R> buildPluginResponse(Tuple4<Optional<T>, Optional<Update>, Optional<CriteriaDefinition>, Optional<R>> tuple, R2dbcPluginContext<T, R> context) {
        return R2dbcPluginResponse.<T, R>builder()
                .entity(tuple.getT1().orElse(context.getEntity()))
                .update(tuple.getT2().orElse(context.getUpdate()))
                .criteria(tuple.getT3().orElse(context.getCriteria()))
                .result(tuple.getT4().orElse(context.getResult()))
                .build();
    }

    private <T> Mono<Optional<T>> handleEntityIfMatch(R2dbcPluginContext<T, ?> context) {
        return Mono.just(context)
                .filter(it -> it.getEntity() != null && this instanceof R2dbcEntityPlugin)
                .flatMap(it -> {
                    R2dbcEntityPlugin<T> entityPlugin = ReflectionKit.cast(this);
                    R2dbcPluginEnum.Slot slot = R2dbcPluginEnum.Slot.fromExecutor(context.getExecutorClass());
                    return entityPlugin.handleEntity(slot, context.getEntity())
                            .map(Optional::of);
                })
                .defaultIfEmpty(Optional.empty())
                .onErrorReturn(Optional.empty());
    }

    private <T> Mono<Optional<Update>> handleUpdateIfMatch(R2dbcPluginContext<T, ?> context) {
        return Mono.just(context)
                .filter(it -> this instanceof R2dbcUpdatePlugin)
                .flatMap(it -> {
                    R2dbcUpdatePlugin<T> updatePlugin = ReflectionKit.cast(this);
                    return updatePlugin.handleUpdate(context.getEntityType(), context.getUpdate())
                            .map(Optional::of)
                            .defaultIfEmpty(Optional.empty())
                            .onErrorReturn(Optional.empty());
                })
                .defaultIfEmpty(Optional.empty())
                .onErrorReturn(Optional.empty());
    }

    private <T, R> Mono<Optional<CriteriaDefinition>> handleCriteriaIfMatch(R2dbcPluginContext<T, R> context) {
        return Mono.just(context)
                .filter(it -> this instanceof R2dbcCriteriaPlugin)
                .flatMap(it -> {
                    R2dbcCriteriaPlugin<T> criteriaPlugin = ReflectionKit.cast(this);
                    return criteriaPlugin.handleCriteria(context.getEntityType(), context.getCriteria())
                            .map(Optional::of);
                })
                .defaultIfEmpty(Optional.empty())
                .onErrorReturn(Optional.empty());
    }

    private <T, R> Mono<Optional<R>> handleResultIfMatch(R2dbcPluginContext<T, R> context) {
        return Mono.just(context)
                .filter(it -> this instanceof R2dbcResultPlugin)
                .flatMap(it -> {
                    R2dbcResultPlugin<T, R> resultPlugin = ReflectionKit.cast(this);
                    return resultPlugin.handleResult(context.getEntityType(), context.getResult())
                            .map(Optional::of);
                })
                .defaultIfEmpty(Optional.empty())
                .onErrorReturn(Optional.empty());
    }

}
