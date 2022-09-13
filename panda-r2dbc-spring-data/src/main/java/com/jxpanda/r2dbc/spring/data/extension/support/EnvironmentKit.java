//package com.jxpanda.r2dbc.spring.data.extension.support;
//
//import org.jetbrains.annotations.NotNull;
//import org.springframework.context.EnvironmentAware;
//import org.springframework.core.env.Environment;
//import org.springframework.stereotype.Component;
//
//@Component
//public class EnvironmentKit implements EnvironmentAware {
//
//    private static Environment environment;
//
//    public static void init(Environment environment) {
//        EnvironmentKit.environment = environment;
//    }
//
//    public static <T> T getProperty(String key, Class<T> propertyType) {
//        return environment.getProperty(key, propertyType);
//    }
//
//    public static <T> T getOrDefault(String key, T defaultProperty, Class<T> propertyType) {
//        if (environment == null) {
//            return defaultProperty;
//        }
//        T property = getProperty(key, propertyType);
//        return property == null ? defaultProperty : property;
//    }
//
//    @Override
//    public void setEnvironment(@NotNull Environment environment) {
//        EnvironmentKit.environment = environment;
//    }
//}
