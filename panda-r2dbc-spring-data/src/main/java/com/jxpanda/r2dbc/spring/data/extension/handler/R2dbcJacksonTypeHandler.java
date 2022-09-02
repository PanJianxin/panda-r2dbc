package com.jxpanda.r2dbc.spring.data.extension.handler;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

import java.lang.reflect.Type;

public class R2dbcJacksonTypeHandler<T> extends R2dbcJsonTypeHandler<T, String> {

    /**
     * jackson的objectMapper
     */
    private final ObjectMapper objectMapper;

    /**
     * 默认构造器使用内置的objectMapper
     * 内部默认的objectMapper暂时不支持配置
     * 如果有配置需求，使用另外一个构造器来替换内部的objectMapper实现来解决
     * 内置的objectMapper只实现简单的功能，不过多配置
     * 即：只保证对象能被序列化/反序列化而不报错
     */
    public R2dbcJacksonTypeHandler() {
        this.objectMapper = new ObjectMapper();
        // 简单配置一下， 保证不会报错即可
        this.objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        this.objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        this.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 支持使用外部构造好的objectMapper来替换
     */
    public R2dbcJacksonTypeHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @SneakyThrows
    protected T readFromJson(String json, RelationalPersistentProperty property) {
        return objectMapper.readValue(json, new TypeReference<>() {
            @Override
            public Type getType() {
                assert property.getField() != null;
                return property.getField().getGenericType();
            }
        });
    }


    @Override
    @SneakyThrows
    protected String writeToJson(T object) {
        return objectMapper.writeValueAsString(object);
    }


}
