package com.jxpanda.r2dbc.spring.data.core.enhance.plugin;

import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.annotation.R2dbcPluginSlot;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.contract.R2dbcAfterPlugin;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.contract.R2dbcBeforePlugin;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model.R2dbcPluginContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 插件执行器：负责把“beforeSQL 阶段”的插件与“afterSQL 阶段”的插件分组，
 * 并按 order 顺序依次 apply(...)。
 */
public class R2dbcPluginExecutor {

    private final List<R2dbcOperationPlugin> plugins;

    public R2dbcPluginExecutor() {
        this(new ArrayList<>());
    }

    public R2dbcPluginExecutor(List<R2dbcOperationPlugin> plugins) {
        this.plugins = plugins;
    }

    /** 注册一个插件 */
    public R2dbcPluginExecutor addPlugin(R2dbcOperationPlugin plugin) {
        plugins.add(plugin);
        return this;
    }

    public R2dbcPluginExecutor addPlugins(List<R2dbcOperationPlugin> plugins) {
        this.plugins.addAll(plugins);
        return this;
    }

    public <T, R> Mono<R2dbcPluginContext<T, R>> runPlugin(R2dbcPluginContext<T, R> context, String pluginName) {
        return run(context, it -> it.getPluginName().equals(pluginName));
    }

    /**
     * 运行“beforeSQL 阶段”的所有插件。
     * 传入的 ctx.rawResult 通常为 null，因为 SQL 还没执行。
     */
    public <T, R> Mono<R2dbcPluginContext<T, R>> runBefore(R2dbcPluginContext<T, R> context) {
        return run(context, it -> it instanceof R2dbcBeforePlugin);
    }

    /**
     * 运行“afterSQL 阶段”的所有插件。
     * 传入的 ctx.rawResult 已经是 SQL 返回值（例如更新条数、查询结果）。
     */
    public <T, R> Mono<R2dbcPluginContext<T, R>> runAfter(R2dbcPluginContext<T, R> context) {
        return run(context, it -> it instanceof R2dbcAfterPlugin);
    }


    private <T, R> Mono<R2dbcPluginContext<T, R>> run(R2dbcPluginContext<T, R> context, Predicate<R2dbcOperationPlugin> predicate) {
        return Flux.fromIterable(plugins)
                .filter(plugin -> {
                    R2dbcPluginSlot slot = plugin.getClass().getAnnotation(R2dbcPluginSlot.class);
                    if (slot == null) {
                        return false;
                    }
                    return Arrays.stream(slot.values())
                            .anyMatch(it -> it.isSupport(context.getExecutorClass()));
                })
                .filter(predicate)
                .sort(Comparator.comparingInt(R2dbcOperationPlugin::getSort))
                .reduce(Mono.just(context), (contextMono, plugin) -> contextMono.flatMap(plugin::apply))
                .flatMap(Function.identity());

    }

    /**
     * 根据插件名称移除
     */
    public void removePlugin(String pluginName) {
        plugins.removeIf(it -> it.getPluginName().equals(pluginName));
    }
}

