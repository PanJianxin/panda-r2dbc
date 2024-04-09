package com.jxpanda.r2dbc.spring.data.core.enhance.plugin;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Panda
 */
@Getter
public abstract class R2dbcOperationPlugin {

    private final String group;

    private final String name;

    @Setter
    private int sort;

    public R2dbcOperationPlugin(String group, String name) {
        this.group = group;
        this.name = name;
        this.sort = 0;
    }

    public <T, R, PR> R2dbcPluginContext<T, R, PR> apply(R2dbcPluginContext<T, R, PR> context) {
        if (context.isEnable()) {
            return plugInto(context);
        }
        return context;
    }


    protected abstract <T, R, PR> R2dbcPluginContext<T, R, PR> plugInto(R2dbcPluginContext<T, R, PR> parameter);


}
