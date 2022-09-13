package demo.model;

import com.baomidou.mybatisplus.core.toolkit.ClassUtils;
import com.baomidou.mybatisplus.core.toolkit.reflect.GenericTypeUtils;
import com.jxpanda.r2dbc.spring.data.core.R2dbcEntityTemplate;
import com.jxpanda.r2dbc.spring.data.extension.Entity;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;


public class BaseService<T extends Entity> {

    @SuppressWarnings("unchecked")
    private final Class<T> clazz = (Class<T>) getSuperClassGenericType(this.getClass(), BaseService.class, 0);

    @Resource
    private R2dbcEntityTemplate r2dbcEntityTemplate;

    public Mono<T> selectById(String id) {
//        r2dbcEntityTemplate.select(Query.query(Criteria.where("sum()")))
        return r2dbcEntityTemplate.select(clazz).matching(Query.query(Criteria.where(T.ID).is(id))).one();
//        return r2dbcEntityTemplate.selectOne(Query.query(Criteria.where(T.ID).is(id)), clazz)
//                .log();
    }

    public static Class<?> getSuperClassGenericType(final Class<?> clazz, final Class<?> genericIfc, final int index) {
        //update by noear @2021-09-03
        Class<?>[] typeArguments = GenericTypeUtils.resolveTypeArguments(ClassUtils.getUserClass(clazz), genericIfc);
        return null == typeArguments ? null : typeArguments[index];
    }

}
