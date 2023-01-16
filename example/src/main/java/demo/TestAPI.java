package demo;

import com.jxpanda.commons.toolkit.IdentifierKit;
import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.core.query.LambdaCriteria;
import demo.model.*;
import io.r2dbc.spi.Connection;
import lombok.AllArgsConstructor;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("test")
@AllArgsConstructor
public class TestAPI {

    private final UserRepository userRepository;

    private final ReactiveEntityTemplate reactiveEntityTemplate;

    private final OrderService orderService;


//    @PostMapping("")
//    public Mono<User> save(@RequestBody Mono<User> userMono) {
//        return userMono
//                .flatMap(user -> {
//                    if (ObjectUtils.isEmpty(user.getId())) {
//                        user.setId(IdentifierKit.nextIdString());
//                        return r2dbcEntityTemplate.insert(user);
//                    }
//                    return userRepository.save(user);
//                });
//    }

    @GetMapping("{userId}")
    public Mono<User> getUser(@PathVariable("userId") String userId) {
        return userRepository.findById(userId);
    }

    @GetMapping(path = "select", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Order> selectList(int duration) {
        return reactiveEntityTemplate.select(Order.class)
                .all()
                .delayElements(Duration.of(duration, ChronoUnit.SECONDS));
    }

    @PostMapping("/order/insert")
    public Mono<Order> insert(@RequestBody Order order) {
//        return orderService.update(order, Query.query(Criteria.where(Order.ID).is(orderId)));
        return orderService.insert(order)
                .doOnSuccess(o -> {
                    System.out.println(o);
                });
    }

    @PostMapping("/order/insert-batch")
    public Flux<Order> insertBatch(@RequestBody List<Order> orderList) {
//        return orderService.update(order, Query.query(Criteria.where(Order.ID).is(orderId)));
        return reactiveEntityTemplate.insert(Order.class)
                .batchInsert(orderList);
    }

    @PostMapping("/order/update")
    public Mono<Order> update(@RequestBody Order order, String orderId) {
//        return orderService.update(order, Query.query(Criteria.where(Order.ID).is(orderId)));
//        return r2dbcEntityTemplate.insert(order);
        return reactiveEntityTemplate.update(Order.class).matching(Query.query(LambdaCriteria.where(Order::getId).is(orderId)))
                .apply(Update.update(Order.AMOUNT, order.getAmount()))
                .map(l -> {
                    order.setId(orderId);
                    return order;
                });
    }

    @GetMapping("/order/{id}")
    public Mono<Order> getOrder(@PathVariable("id") String id) {
        return orderService.selectById(id)
                .doOnSuccess(order -> {
                    if (order != null) {
                        orderService.forEachColum(order, System.out::println);
                    }
                });
    }


    @GetMapping("/order/sum")
    public Mono<OrderSum> getOrderSum() {
        return reactiveEntityTemplate.select(OrderSum.class)
                .matching(Query.query(LambdaCriteria.where(Order::getId).greaterThan("3005542952022835202")))
                .one();
//        return r2dbcEntityTemplate.getDatabaseClient().sql("SELECT count(*), sum(amount) FROM `order`")
//                .fetch()
//                .one();
    }

    @GetMapping("/order/delete/{id}")
    public Mono<Order> delete(@PathVariable("id") String id) {
        return reactiveEntityTemplate.delete(Order.builder()
                .id(id)
                .build());
//        return r2dbcEntityTemplate.getDatabaseClient().sql("SELECT count(*), sum(amount) FROM `order`")
//                .fetch()
//                .one();
    }

    @GetMapping("/order/destroy/{id}")
    public Mono<OrderSum> destroy(@PathVariable("id") String id) {
        return reactiveEntityTemplate.select(OrderSum.class)
                .matching(Query.query(Criteria.where(Order.ID).greaterThan("3005542952022835202")))
                .one();
//        return r2dbcEntityTemplate.getDatabaseClient().sql("SELECT count(*), sum(amount) FROM `order`")
//                .fetch()
//                .one();
    }

    @Transactional
    @GetMapping("/transaction")
    public Mono<?> transaction(String userId) {
        User user = User.builder()
                .id(userId)
                .phone(IdentifierKit.nextIdString().substring(0, 11))
                .build();
//        Mono<User> userMono = Mono.just();
        Order order = Order.builder()
                .id(IdentifierKit.nextIdString())
                .number(IdentifierKit.nextIdString())
                .userId(userId)
//                .extend(Collections.emptyList())
//                .amountChanges(new ArrayList<>())
                .build();

        reactiveEntityTemplate.update(user).map(user1 -> {
            return user1;
        });

        return Flux.merge(reactiveEntityTemplate.insert(user),
                        reactiveEntityTemplate.insert(order))
                .parallel(2)
                .map(it -> {
                    System.out.println(it);
                    return it;
                })
                .sequential()
                .collectList();

//        return r2dbcEntityTemplate.insert(order)
//                .zipWith(r2dbcEntityTemplate.insert(user))
//                .publishOn(ForkJoinPoolScheduler.create("ForkJoin"))
//                .log()
////                .onErrorContinue((err, obj) -> {
////                    System.out.println(err.getMessage());
////                    System.out.println(obj);
////                })
//                .map(it -> it);
    }


}
