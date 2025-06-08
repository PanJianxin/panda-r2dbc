package com.jxpanda.r2dbc.spring.data.extension.service;

import java.beans.Introspector;

public class ReactiveEntityRepositoryNameResolver {

    private static final String DEFAULT_SUFFIX = "ReactiveEntityRepository";

    public static String resolve(Class<?> entityClass) {
        return Introspector.decapitalize(entityClass.getSimpleName()) + DEFAULT_SUFFIX;
    }

}
