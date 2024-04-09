package com.jxpanda.r2dbc.spring.data.core.enhance.plugin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Panda
 */
@Getter
@AllArgsConstructor
public class R2dbcPluginExecutor {

    private final Map<String, R2dbcOperationPlugin> pluginCacheUsingName;

    public R2dbcPluginExecutor() {
        this.pluginCacheUsingName = new HashMap<>(8);
    }

    public R2dbcPluginExecutor addPlugin(R2dbcOperationPlugin plugin) {
        pluginCacheUsingName.put(plugin.getName(), plugin);
        return this;
    }

    public void removePlugin(R2dbcPluginName pluginName) {
        pluginCacheUsingName.remove(pluginName.getName());
    }

    public R2dbcOperationPlugin getPlugin(R2dbcPluginName pluginName) {
        return pluginCacheUsingName.get(pluginName.getName());
    }

    public <T, R, PR> R2dbcPluginContext<T, R, PR> run(R2dbcPluginContext<T, R, PR> context) {
        return getPlugin(context.getPluginName()).apply(context);
    }

}
