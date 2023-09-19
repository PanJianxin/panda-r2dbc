package demo.api;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.criteria.EnhancedCriteria;
import demo.model.Order;
import lombok.AllArgsConstructor;
import org.springframework.data.relational.core.query.Query;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Panda
 */
@RestController
@RequestMapping("select")
@AllArgsConstructor
public class SelectAPI {

    private final ReactiveEntityTemplate reactiveEntityTemplate;

    @GetMapping("exists")
    public Mono<Boolean> exists(String id) {
        return reactiveEntityTemplate.select(Order.class)
                .matching(Query.query(EnhancedCriteria.where(Order::getId).is(id)))
                .exists();
    }

    @GetMapping("count")
    public Mono<Long> count() {
        return reactiveEntityTemplate.select(Order.class)
                .count();
    }

    @GetMapping("detail/{id:\\d+}")
    public Mono<Order> detail(@PathVariable("id") String id) {
        return reactiveEntityTemplate.select(Order.class)
                .byId(id);
    }

    @GetMapping("list")
    public Flux<Order> list() {
        return reactiveEntityTemplate.select(Order.class)
                .all();
    }


}
