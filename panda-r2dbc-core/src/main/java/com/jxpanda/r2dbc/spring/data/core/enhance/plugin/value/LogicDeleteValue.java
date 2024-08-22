package com.jxpanda.r2dbc.spring.data.core.enhance.plugin.value;

import com.jxpanda.r2dbc.spring.data.infrastructure.constant.StringConstant;
import lombok.*;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogicDeleteValue {

    private static final Map<String, PluginValueHandler> HANDLER_CACHE = new HashMap<>();

    private LogicDeleteValueType type;

    private String deleteValue;

    private Class<? extends PluginValueHandler> deleteValueHandler;

    private String undeleteValue;

    private Class<? extends PluginValueHandler> undeleteValueHandler;

    public static LogicDeleteValue empty() {
        return new LogicDeleteValue(LogicDeleteValueType.CUSTOMER, StringConstant.BLANK, null, StringConstant.BLANK, null);
    }

    public Object getDeleteValue() {
        return getValue(deleteValue, deleteValueHandler);
    }

    public Object getUndeleteValue() {
        return getValue(undeleteValue, undeleteValueHandler);
    }

    public static Object getValue(String value, Class<? extends PluginValueHandler> handlerClass) {
        PluginValueHandler valueHandler = HANDLER_CACHE.computeIfAbsent(value, key -> {
            if (handlerClass != null) {
                try {
                    return handlerClass.getConstructor().newInstance();
                } catch (Exception ignored) {
                }
            }
            return new DefaultValueHandler();
        });
        return valueHandler.covert(value);
    }

    public static class CurrentTimestampHandler implements PluginValueHandler {
        @NonNull
        public Object covert(@NonNull String value) {
            if (value.equals("CURRENT_TIMESTAMP")) {
                return LocalDateTime.now();
            }
            return value;
        }
    }

}
