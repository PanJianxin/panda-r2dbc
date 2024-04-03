package com.jxpanda.r2dbc.spring.data.core.enhance.handler;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * @author Panda
 */
@Slf4j
@AllArgsConstructor
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

    @Override
    protected T readFromJson(byte[] jsonBytes, RelationalPersistentProperty property) {
        try {
            return objectMapper.readValue(jsonBytes, typeReference(property));
        } catch (IOException e) {
            log.error("[JSON READ ERROR]", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected T readFromJson(String json, RelationalPersistentProperty property) {
        try {
            return objectMapper.readValue(json, typeReference(property));
        } catch (JsonProcessingException e) {
            log.error("[JSON READ ERROR]", e);
            throw new RuntimeException(e);
        }
    }


    @Override
    protected String writeToJson(T object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("[JSON WRITE ERROR]", e);
            throw new RuntimeException(e);
        }
    }

    private TypeReference<T> typeReference(RelationalPersistentProperty property) {
        return new TypeReference<>() {
            @Override
            public Type getType() {
                assert property.getField() != null;
                return property.getField().getGenericType();
            }
        };
    }


}
