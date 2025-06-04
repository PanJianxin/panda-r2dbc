package com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.relational.core.query.CriteriaDefinition;
import org.springframework.data.relational.core.query.Update;
import org.springframework.lang.Nullable;

@Getter
@Setter
@Builder
@RequiredArgsConstructor
public class R2dbcPluginResponse<T, R> {

    @Nullable
    private final T entity;
    @Nullable
    private final R result;
    @Nullable
    private final Update update;
    @Nullable
    private final CriteriaDefinition criteria;

}
