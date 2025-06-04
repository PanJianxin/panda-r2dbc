package com.jxpanda.r2dbc.spring.data.core.enhance.plugin.contract;

import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model.R2dbcPluginEnum;
import reactor.core.publisher.Mono;

public interface R2dbcEntityPlugin<T> extends R2dbcBeforePlugin {

    /** 处理实体，返回修改后的实体 */
    Mono<T> handleEntity(R2dbcPluginEnum.Slot usingSlot, T entity);

}
