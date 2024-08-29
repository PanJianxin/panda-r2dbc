package com.jxpanda.r2dbc.spring.data.core.enhance.handler;

import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.lang.Nullable;

import java.util.Map;

/**
 * R2dbc自定义类型处理器管理器，用于注册和获取自定义的类型处理器
 * 通过此管理器，可以实现对数据库字段读写逻辑的自定义处理
 * 主要是为了处理一些特殊类型的字段，比如枚举类型和Json类型
 *
 * @author Panda
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class R2dbcCustomTypeHandlers {

    // 自定义类型处理器缓存
    private final HandlerCache handlerCache = new HandlerCache();

    /**
     * 注册自定义类型处理器
     *
     * @param typeHandlerClass 类型处理器的类类型
     * @param handler          具体的类型处理器实例
     */
    public void register(Class<? extends R2dbcTypeHandler> typeHandlerClass, R2dbcTypeHandler handler) {
        handlerCache.put(typeHandlerClass, handler.getValueClass(), handler);
    }

    /**
     * 判断给定的属性是否有对应的自定义类型处理器
     *
     * @param property 数据库映射属性
     * @return 是否有对应的自定义类型处理器
     */
    public boolean hasTypeHandler(RelationalPersistentProperty property) {
        // 获取字段上的TableColumn注解
        TableColumn tableColumn = property.findAnnotation(TableColumn.class);
        // 如果没有注解，返回false
        if (tableColumn == null) {
            return false;
        }
        // 获取注解上配置的类型处理器
        Class<? extends R2dbcTypeHandler> typeHandler = tableColumn.typeHandler();
        // 如果主动设置了忽略，返回false
        if (typeHandler.equals(R2dbcTypeHandler.IgnoreHandler.class)) {
            return false;
        }
        // 如果有自定义的类型处理器，就返回true
        if (!typeHandler.equals(R2dbcTypeHandler.DefaultHandler.class)) {
            return true;
        }
        // 默认情况下处理枚举和Json类型
        return property.getType().isEnum() || tableColumn.isJson();
    }

    /**
     * 获取类型处理器
     *
     * @param value    字段值
     * @param property 数据库映射属性
     * @return 对应的类型处理器实例
     */
    private R2dbcTypeHandler getTypeHandler(Object value, RelationalPersistentProperty property) {
        // 获取字段上的TableColumn注解
        TableColumn tableColumn = property.getRequiredAnnotation(TableColumn.class);
        // 获取注解上配置的类型处理器
        Class<? extends R2dbcTypeHandler> typeHandlerClass = tableColumn.typeHandler();
        // 判断是否是默认的类型处理器
        boolean isDefault = typeHandlerClass.equals(R2dbcTypeHandler.DefaultHandler.class);
        // 获取字段值的类类型
        Class<?> valueType = value.getClass();
        if (isDefault) {
            // 如果是枚举类型，则获取对应的枚举类型处理器
            if (property.getType().isEnum()) {
                return handlerCache.getEnumHandler(valueType);
            }
            // 如果是Json类型，则获取对应的Json类型处理器
            else if (tableColumn.isJson()) {
                return handlerCache.getJsonHandler(valueType);
            }
        }
        // 获取或新建实例化自定义的类型处理器
        return handlerCache.getOrNewInstance(typeHandlerClass, valueType, typeHandlerClass);
    }

    /**
     * 读取字段值，通过类型处理器
     * 能调用这个函数，说明一定经过了hasCustomReadTarget函数的检查
     * 因此这里不重复判断是否能处理了
     *
     * @param value    字段值
     * @param property 数据库映射属性
     * @return 通过类型处理器处理后的字段值
     */
    public Object read(Object value, RelationalPersistentProperty property) {
        return getTypeHandler(value, property).read(value, property);
    }

    /**
     * 写入字段值，通过类型处理器
     *
     * @param value    字段值，可能为null
     * @param property 数据库映射属性
     * @return 通过类型处理器处理后的字段值，如果输入值为null，则返回null
     */
    @Nullable
    public Object write(@Nullable Object value, RelationalPersistentProperty property) {
        if (value == null) {
            return null;
        }
        return getTypeHandler(value, property).write(value, property);
    }

    /**
     * 自定义类型处理器缓存类，用于缓存不同类型处理器的实例
     * 通过缓存机制，避免重复实例化相同类型的处理器，提高性能
     */
    private static class HandlerCache extends TwoKeyMap<Class<? extends R2dbcTypeHandler>, Class<?>, R2dbcTypeHandler> {

        /**
         * 获取并缓存枚举类型处理器
         *
         * @param valueType 枚举值的类类型
         * @return 枚举类型处理器实例
         */
        private R2dbcTypeHandler getEnumHandler(Class<?> valueType) {
            return getOrDefault(R2dbcEnumTypeHandler.class, valueType, new R2dbcEnumTypeHandler());
        }

        /**
         * 获取并缓存Json类型处理器
         *
         * @param valueType Json值的类类型
         * @return Json类型处理器实例
         */
        private R2dbcTypeHandler getJsonHandler(Class<?> valueType) {
            Map<Class<?>, R2dbcTypeHandler> levelOneCache = getLevelOneCache(R2dbcJsonTypeHandler.class);
            // 计算或获取缓存中的Json处理器实例
            // FIXME: 这里之所以要用父类去查找缓存的原因是为了兼容PostgresSQL数据库
            //  因为PostgresSQL数据库的Json类型返回来之后，是PG驱动包中的Json对象，而非字符串
            //  这导致了只有依赖了PG数据库驱动的时候才会有对应的类，因此要做特殊处理来兼容
            //  Spring中可以使用@ConditionalOnClass注解来判断是否依赖了PG数据库驱动包，来加载PG的类型处理器
            //  由于PG的Json类有很多子类，而这些子类并不是public的，无法直接构建缓存，因此这里要通过父类去查找缓存
            return levelOneCache.computeIfAbsent(valueType, (key) -> levelOneCache.getOrDefault(getTopLevelClass(valueType), new R2dbcJacksonTypeHandler()));
        }

        /**
         * 获取最顶层的类
         *
         * @param clazz 当前类
         * @return 最顶层的类
         */
        private Class<?> getTopLevelClass(Class<?> clazz) {
            // 递归获取最顶层的父类
            Class<?> superclass = clazz.getSuperclass();
            if (superclass == null || superclass.equals(Object.class)) {
                return clazz;
            }
            return getTopLevelClass(superclass);
        }

    }

}
