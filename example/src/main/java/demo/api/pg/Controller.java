package demo.api.pg;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.page.Pagination;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.seeker.Seeker;
import demo.model.pg.Entity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 控制器接口，为CRUD操作提供默认方法。
 *
 * @param <T> 继承自Entity的实体类类型。
 * @author Panda
 */
public interface Controller<T extends Entity> {

    Map<Class<?>, Class<?>> GENERIC_TYPE_MAP = new ConcurrentHashMap<>(8);

    UnsupportedOperationException UNSUPPORTED_OPERATION = new UnsupportedOperationException("服务器拒绝执行当前操作");

    /**
     * 获取操作的实体类类型。
     *
     * @return 实体类的Class对象。
     */
    @SuppressWarnings("unchecked")
    default Class<T> getEntityClass() {
        Class<?> clazz = this.getClass();
        return (Class<T>) GENERIC_TYPE_MAP.computeIfAbsent(clazz, key -> {
            try {
                return getGenericType(clazz);
            } catch (Exception e) {
                return null;
            }
        });
    }


    /**
     * 获取ReactiveEntityTemplate实例，用于数据库操作。
     *
     * @return ReactiveEntityTemplate实例。
     */
    ReactiveEntityTemplate reactiveEntityTemplate();

    /**
     * 保存实体。
     *
     * @param entity 待保存的实体对象。如果实体有ID，则更新现有记录；如果没有ID，则创建新记录。
     * @return 保存成功后的实体对象。
     */
    @PostMapping("/save")
    default Mono<T> save(@RequestBody T entity) {
        return reactiveEntityTemplate().save(entity);
    }

    /**
     * 根据ID删除实体。
     *
     * @param id 待删除实体的ID。
     * @return 删除操作的成功与否。
     */
    @DeleteMapping("/delete/{id:\\d+}")
    default Mono<Boolean> delete(@PathVariable("id") String id) {
        return reactiveEntityTemplate()
                .delete(getEntityClass())
                .byId(id);
    }

    /**
     * 根据ID获取实体详情。
     *
     * @param id 待查询实体的ID。
     * @return 查询到的实体对象。
     */
    @GetMapping("/detail/{id:\\d+}")
    default Mono<T> detail(@PathVariable("id") String id) {
        return reactiveEntityTemplate()
                .select(getEntityClass())
                .byId(id);
    }

    /**
     * 根据Seeker查询实体列表。
     *
     * @param seeker 查询条件对象，包含分页和过滤条件。
     * @return 查询结果，包含分页信息和实体列表。
     */
    @PostMapping("/query")
    default Mono<Pagination<T>> query(@RequestBody Seeker<T> seeker) {
        return reactiveEntityTemplate().select(getEntityClass())
                .seek(seeker);
    }


    @SuppressWarnings("unchecked")
    private static <T extends Entity> Class<T> getGenericType(Class<?> clazz) {
        return Stream.of(clazz.getGenericInterfaces())
                .filter(type -> type instanceof ParameterizedType)
                .map(type -> (ParameterizedType) type)
                .filter(parameterizedType -> parameterizedType.getRawType().equals(Controller.class))
                .map(parameterizedType -> (Class<T>) parameterizedType.getActualTypeArguments()[0])
                .findFirst()
                .orElse(null);
    }

}
