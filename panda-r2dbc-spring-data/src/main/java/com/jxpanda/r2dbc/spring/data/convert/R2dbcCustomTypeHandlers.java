package com.jxpanda.r2dbc.spring.data.convert;

import com.jxpanda.r2dbc.spring.data.extension.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.extension.handler.R2dbcEnumTypeHandler;
import com.jxpanda.r2dbc.spring.data.extension.handler.R2dbcJacksonTypeHandler;
import com.jxpanda.r2dbc.spring.data.extension.handler.R2dbcJsonTypeHandler;
import com.jxpanda.r2dbc.spring.data.extension.handler.R2dbcTypeHandler;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings({"rawtypes", "unchecked"})
public class R2dbcCustomTypeHandlers {

    private final Map<Class<? extends R2dbcTypeHandler>, R2dbcTypeHandler> typeHandlerMap;

    public R2dbcCustomTypeHandlers() {
        this.typeHandlerMap = new HashMap<>();
        this.typeHandlerMap.put(R2dbcEnumTypeHandler.class, R2dbcEnumTypeHandler.INSTANCE);
        this.typeHandlerMap.put(R2dbcJsonTypeHandler.class, new R2dbcJacksonTypeHandler());
    }

    public R2dbcCustomTypeHandlers(List<R2dbcTypeHandler> typeHandlerList) {
        this.typeHandlerMap = typeHandlerList.stream().collect(Collectors.toMap(R2dbcTypeHandler::getClass, Function.identity()));
    }

    public boolean hasCustomReadTarget(RelationalPersistentProperty property) {
        TableColumn tableColumn = property.findAnnotation(TableColumn.class);

        if (tableColumn == null) {
            return false;
        }
        // 是否需要处理（目前只有枚举和json需要处理）
        boolean needHandle = property.getType().isEnum() || tableColumn.isJson();
        // 是否强制忽略
        boolean isIgnore = tableColumn.typeHandler().equals(R2dbcTypeHandler.IgnoreHandler.class);
        // 需要处理，也没有被忽略
        return needHandle && !isIgnore;
    }

    /**
     * 能调用这个函数，说明一定经过了hasCustomReadTarget函数的检查
     * 因此这里不重复判断是否能处理了
     */
    public Object read(Object value, RelationalPersistentProperty property) {
        return getTypeHandler(property).read(value, property);
    }


    private R2dbcTypeHandler getTypeHandler(RelationalPersistentProperty property) {
        TableColumn tableColumn = property.getRequiredAnnotation(TableColumn.class);
        Class<? extends R2dbcTypeHandler> typeHandlerClass = tableColumn.typeHandler();
        boolean isDefault = typeHandlerClass.equals(R2dbcTypeHandler.DefaultHandler.class);
        if (isDefault) {
            if (property.getType().isEnum()) {
                typeHandlerClass = R2dbcEnumTypeHandler.class;
            } else if (tableColumn.isJson()) {
                typeHandlerClass = R2dbcJsonTypeHandler.class;
            }
        }
        return typeHandlerMap.get(typeHandlerClass);
    }

}
