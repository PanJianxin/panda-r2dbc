package com.jxpanda.r2dbc.spring.data.extension.entity;

import com.jxpanda.r2dbc.spring.data.extension.service.ReactiveEntityRepositoryNameResolver;

public interface CrudEntity {

    static String serviceBeanName(Class<?> clazz) {
        return ReactiveEntityRepositoryNameResolver.resolve(clazz);
    }

    default String serviceBeanName() {
        return serviceBeanName(this.getClass());
    }
}