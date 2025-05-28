package com.jxpanda.r2dbc.spring.data.extension.service;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.extension.entity.Entity;
import lombok.Getter;

/**
 * @author Panda
 */
@Getter
@SuppressWarnings("ClassCanBeRecord")
public class DefaultReactiveEntityService<T extends Entity> implements ReactiveEntityService<T> {

    private final ReactiveEntityTemplate reactiveEntityTemplate;

    private final Class<T> entityClass;

    public DefaultReactiveEntityService(ReactiveEntityTemplate reactiveEntityTemplate, Class<T> entityClass) {
        this.reactiveEntityTemplate = reactiveEntityTemplate;
        this.entityClass = entityClass;
    }


}
