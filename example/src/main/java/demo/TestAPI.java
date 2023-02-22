package demo;

import com.jxpanda.commons.toolkit.IdentifierKit;
import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.criteria.EnhancedCriteria;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.page.Pagination;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.page.Paging;
import demo.model.*;
import lombok.AllArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.*;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Stream;

@RestController
@RequestMapping("test")
@AllArgsConstructor
public class TestAPI {

    private final UserRepository userRepository;

    private final ReactiveEntityTemplate reactiveEntityTemplate;

    private final OrderService orderService;


    @GetMapping("/page")
    public Mono<Pagination<Order>> paginationMono(Long current, Integer size, Boolean isQueryCount) {
        return reactiveEntityTemplate.select(Order.class)
                .paging(new Paging(current, size, isQueryCount));
    }

    @GetMapping("{userId}")
    public Mono<User> getUser(@PathVariable("userId") String userId) {
        return userRepository.findById(userId);
    }

    @GetMapping(path = "select", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Order> selectList(int duration) {
        return reactiveEntityTemplate.select(Order.class)
                .matching(Query.query(EnhancedCriteria.where(Order::getId).greaterThan("206975615562153985")).limit(2).offset(0))
                .all()
                .delayElements(Duration.of(duration, ChronoUnit.SECONDS));
    }

    @PostMapping("/order/insert")
    public Mono<Order> insert(@RequestBody Order order) {
        return orderService.insert(order)
                .log();
    }

    @PostMapping("/order/save")
    public Mono<Order> save(@RequestBody Order order) {
        order.setNumber(IdentifierKit.nextIdString());
        return reactiveEntityTemplate.save(Order.class)
                .using(order)
                .log();
    }

    @PostMapping("/order/item/insert")
    public Mono<OrderItem> itemInsert(@RequestBody OrderItem orderItem) {
        return reactiveEntityTemplate.insert(OrderItem.class)
                .using(orderItem)
                .log();
    }

    @PostMapping("/order/insert-batch")
    public Flux<Order> insertBatch(@RequestBody List<Order> orderList) {
        return reactiveEntityTemplate.insert(Order.class)
                .batch(orderList);
    }

    @PostMapping("/order/save-batch")
    public Flux<Order> saveBatch(@RequestBody List<Order> orderList) {
        return reactiveEntityTemplate.save(Order.class)
                .batch(orderList);
    }

    @PostMapping("/order/update")
    public Mono<Order> update(@RequestBody Order order, String orderId) {
        return reactiveEntityTemplate.update(Order.class).matching(Query.query(EnhancedCriteria.where(Order::getId).is(orderId)))
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
                .matching(Query.query(EnhancedCriteria.where(Order::getId).greaterThan("3005542952022835202")))
                .one();
    }

    @GetMapping("/order/delete/{id}")
    public Mono<Order> delete(@PathVariable("id") String id) {
        return reactiveEntityTemplate.delete(Order.builder()
                .id(id)
                .build());
    }

    @DeleteMapping("/order/destroy/{id}")
    public Mono<Boolean> destroy(@PathVariable("id") String id) {
        return reactiveEntityTemplate.destroy(OrderSum.class)
                .using(Order.builder().id(id).build());
    }

    @DeleteMapping("/order/destroy/all")
    public Mono<Long> destroy() {
        return reactiveEntityTemplate.destroy(OrderSum.class)
                .matching(Query.query(Criteria.where(Order.ID).greaterThan("3007718830303608834")))
                .all();
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
    }


}
