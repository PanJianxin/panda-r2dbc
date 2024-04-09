package com.jxpanda.r2dbc.spring.data.core.operation.executor;

import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcPluginContext;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcPluginName;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Panda
 */
@Getter
public class R2dbcOperationOption {

//    private R2dbcOperationParameter<T, R> operationParameter;

    private final boolean selectReference;

    private final Map<R2dbcPluginName, Boolean> pluginSwitch;


    public R2dbcOperationOption() {
        this(true);
    }

    public R2dbcOperationOption(boolean selectReference) {
        this.selectReference = selectReference;
        this.pluginSwitch = Arrays.stream(R2dbcPluginName.values())
                .collect(Collectors.toMap(Function.identity(), pluginName -> true));
    }

    public boolean isPluginEnable(R2dbcPluginName pluginName) {
        return pluginSwitch.get(pluginName);
    }

    public void disablePlugin(R2dbcPluginName pluginName) {
        pluginSwitch.put(pluginName, false);
    }

    public void enablePlugin(R2dbcPluginName pluginName) {
        pluginSwitch.put(pluginName, true);
    }

}
