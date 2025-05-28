package com.jxpanda.r2dbc.spring.data.extension.service;

import java.beans.Introspector;

public class EntityServiceNameResolver {

    private static final String DEFAULT_SUFFIX = "ReactiveService";

    public static String resolve(Class<?> entityClass) {
        return Introspector.decapitalize(entityClass.getSimpleName()) + DEFAULT_SUFFIX;
    }

}
