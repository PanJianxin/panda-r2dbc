package com.jxpanda.r2dbc.spring.data.core.enhance.plugin;

import lombok.*;

import java.util.Optional;
import java.util.function.Function;

/**
 * 插件执行所需要的上下文对象
 */
@Getter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class R2dbcPluginContext<T, R, PR> {

    private final R2dbcPluginName pluginName;

    private final boolean enable;

    private final Class<T> domainType;

    private final Class<R> returnType;

    private final Class<PR> pluginResultType;

    /**
     * 插件是否被执行过
     */
    @Builder.Default
    private boolean executed = false;

    /**
     * 插件设计为链式调用，这个是上一个插件的结果
     */
    private PR lastPluginResult;

    /**
     * 当前插件执行的结果
     */
    @Getter(AccessLevel.PRIVATE)
    private PR result;


    @SuppressWarnings("unchecked")
    public <NPR> R2dbcPluginContext<T, R, PR> execute(Function<R2dbcPluginContext<T, R, NPR>, NPR> pluginMethod) {
        this.result = (PR) pluginMethod.apply((R2dbcPluginContext<T, R, NPR>) this);
        this.executed = true;
        return this;
    }

    public Optional<PR> takeResult() {
        return takeResult(it -> it);
    }

    public <CR> Optional<CR> takeResult(Function<PR, CR> resultMapper) {
        // 如果插件被执行过才返回结果
        if (isExecuted()) {
            return Optional.ofNullable(resultMapper.apply(this.result));
        }
        return Optional.empty();
    }


}
