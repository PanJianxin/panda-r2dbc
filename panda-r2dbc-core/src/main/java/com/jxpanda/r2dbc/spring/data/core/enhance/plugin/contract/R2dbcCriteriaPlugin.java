package com.jxpanda.r2dbc.spring.data.core.enhance.plugin.contract;

import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Mono;

public interface R2dbcCriteriaPlugin<T> extends R2dbcBeforePlugin {

    Mono<CriteriaDefinition> handleCriteria(Class<T> entityType, @Nullable CriteriaDefinition original);

}
