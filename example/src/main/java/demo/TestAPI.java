package demo;

import com.jxpanda.commons.toolkit.IdentifierKit;
import com.jxpanda.r2dbc.spring.data.core.R2dbcEntityTemplate;
import demo.model.*;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@RestController
@RequestMapping("test")
@AllArgsConstructor
public class TestAPI {

    private final UserRepository userRepository;

    private final R2dbcEntityTemplate r2dbcEntityTemplate;

//    private final OrderRepository orderRepository;

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
    public Flux<User> selectUser() {

        return r2dbcEntityTemplate.select(User.class)
//                .from("user")
                .matching(Query.query(Criteria.where("name").is("Panda")))
                .all();
    }

    @PostMapping("/order/insert")
    public Mono<Order> insert(@RequestBody Order order) {
//        return orderService.update(order, Query.query(Criteria.where(Order.ID).is(orderId)));
        return orderService.insert(order);
//        return r2dbcEntityTemplate.update(Order.class).matching(Query.query(Criteria.where("id").is(orderId)))
//                .apply(Update.update(Order.AMOUNT, order.getAmount()))
//                .map(l -> {
//                    order.setId(orderId);
//                    return order;
//                });
    }

    @PostMapping("/order/update")
    public Mono<Order> update(@RequestBody Order order, String orderId) {
//        return orderService.update(order, Query.query(Criteria.where(Order.ID).is(orderId)));
        return r2dbcEntityTemplate.insert(order);
//        return r2dbcEntityTemplate.update(Order.class).matching(Query.query(Criteria.where("id").is(orderId)))
//                .apply(Update.update(Order.AMOUNT, order.getAmount()))
//                .map(l -> {
//                    order.setId(orderId);
//                    return order;
//                });
    }

    @GetMapping("/order/{id}")
    public Mono<Order> getOrder(@PathVariable("id") String id) {
//        return Mono.just(new Order());
//        return r2dbcEntityTemplate.selectOne(Query.query(Criteria.where(Order.ID).is(id)), Order.class)
//                .log();
        return orderService.selectById(id)
                .doOnSuccess(order -> {
                    orderService.forEachColum(order, columInfo -> {
                        System.out.println(columInfo);
                    });
                })
                .log();
//        return orderRepository.findById(id)
//                .map(it->{
//                    System.out.println(it);
//                    return it;
//                })
//                .log();
    }

    @GetMapping("/order/sum")
    public Mono<OrderSum> getOrderSum() {
        return r2dbcEntityTemplate.select(OrderSum.class)
                .matching(Query.query(Criteria.where(Order.CREATED_DATE).in(LocalDateTime.now(), LocalDateTime.now())))
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

        r2dbcEntityTemplate.update(user).map(user1 -> {
            return user1;
        });

        return Flux.merge(r2dbcEntityTemplate.insert(user),
                        r2dbcEntityTemplate.insert(order))
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
