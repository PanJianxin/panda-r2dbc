package com.jxpanda.r2dbc.spring.data.dialect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.lang.NonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Panda
 */
@SuppressWarnings("ALL")
public class PostgresDialect extends org.springframework.data.r2dbc.dialect.PostgresDialect {


    /**
     * Singleton instance.
     */
    public static final PostgresDialect INSTANCE = new PostgresDialect();


    @Override
    public Collection<Object> getConverters() {
        Collection<Object> converters = super.getConverters();
        converters.add(JsonToMapConverter.INSTANCE);
        return converters;
    }

    @ReadingConverter
    private enum JsonToMapConverter implements Converter<Json, Map<?, ?>> {

        INSTANCE;

        @Override
        @NonNull
        public Map<?, ?> convert(Json source) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                return objectMapper.readValue(source.asString(), new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                return new HashMap<>();
            }
        }
    }

}
