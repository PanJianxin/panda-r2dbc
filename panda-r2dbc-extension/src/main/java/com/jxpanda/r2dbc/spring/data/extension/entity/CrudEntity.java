package com.jxpanda.r2dbc.spring.data.extension.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jxpanda.r2dbc.spring.data.extension.service.EntityServiceNameResolver;

public interface CrudEntity {
    static String getServiceBeanName(Class<?> clazz) {
        return EntityServiceNameResolver.resolve(clazz);
    }

    @JsonIgnore
    default String getServiceBeanName() {
        return getServiceBeanName(this.getClass());
    }
}