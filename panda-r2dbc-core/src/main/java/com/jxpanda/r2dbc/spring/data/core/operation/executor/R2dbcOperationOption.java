package com.jxpanda.r2dbc.spring.data.core.operation.executor;

import lombok.Getter;

import java.util.*;

/**
 * @author Panda
 */
@Getter
public class R2dbcOperationOption {

    private final boolean selectReference;

    private final Set<String> disabledPlugins;


    public R2dbcOperationOption() {
        this(true);
    }

    public R2dbcOperationOption(boolean selectReference) {
        this.selectReference = selectReference;
        this.disabledPlugins = new HashSet<>();
    }


    /**
     * 禁用某个插件：返回一个新的 Context 副本，但把 pluginName 加到 disabledPlugins
     */
    public R2dbcOperationOption disablePlugin(String pluginName) {
        this.disabledPlugins.add(pluginName);
        return this;
    }

}
