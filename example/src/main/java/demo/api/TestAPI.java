package demo.api;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.criteria.EnhancedCriteria;
import demo.model.*;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
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
import java.util.Random;
import java.util.Set;

@RestController
@RequestMapping("test")
@AllArgsConstructor
public class TestAPI {

    private final UserRepository userRepository;

    private final ReactiveEntityTemplate reactiveEntityTemplate;

    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    private final OrderService orderService;

    private final OrderRepository orderRepository;

    @GetMapping("table-vo/{id:\\d+}")
    public Mono<TableVO> tableVO(@PathVariable("id") String id) {
        return reactiveEntityTemplate.select(TableVO.class)
                .matching(Query.query(Criteria.where("id").is(id)))
                .one();
    }

    @GetMapping("/main-table/init")
    public Flux<MainTable> mainTableInit() {
        return Flux.<MainTable>create(sink -> {
            for (int i = 0; i < 1; i++) {
                sink.next(MainTable.builder()
                        .content("content " + i)
                        .subTableIds(List.of("3108098341683068929",
                                "3108098341909561346",
                                "3108098341913755649",
                                "3108098341917949953"
                        ))
                        .jsonObject(new Order.Extend())
                        .build());
            }
            sink.complete();
        }).collectList().flatMapMany(list -> reactiveEntityTemplate.insertBatch(list, MainTable.class));
    }

    @GetMapping(path = "/sub-table/init")
    public Flux<SubTable> subTableInit() {
        return Flux.<SubTable>create(sink -> {
                    for (int i = 0; i < 10; i++) {
                        SubTable subTable = new SubTable();
                        subTable.setContent("content " + i);
                        sink.next(subTable);
                    }
                    sink.complete();
                }).collectList()
                .flatMapMany(list -> reactiveEntityTemplate.insertBatch(list, SubTable.class));
    }

    @GetMapping(path = "/sub-table/list")
    public Flux<SubTable> subTableList() {
        return reactiveEntityTemplate.select(SubTable.class)
                .byIds(Set.of("3108098341683068929", "3108098341909561346", "3108098341913755649", "3108098341917949953"));
    }


    @GetMapping("user/{id:\\d+}")
    public Mono<User> user(@PathVariable("id") String id) {
        return reactiveEntityTemplate.select(User.class)
                .byId(id);
    }

    @GetMapping("vos")
    public Flux<OrderVO> vos() {
        return reactiveEntityTemplate.select(OrderVO.class)
                .all();
    }

    @GetMapping("vo/{id:\\d+}")
    public Mono<OrderVO> vo(@PathVariable("id") String id) {
        return r2dbcEntityTemplate.select(OrderVO.class)
                .matching(Query.query(Criteria.where("id").is(id)))
                .one();
    }

    @GetMapping("join")
    public Flux<OrderDTO> join() {
        return reactiveEntityTemplate.select(OrderDTO.class)
                .all();
    }

    @GetMapping("r2dbc-join")
    public Flux<OrderDTO> r2dbcJoin() {
        return r2dbcEntityTemplate.select(OrderDTO.class)
                .all();
    }

    @GetMapping("/page")
    public Mono<Page<Order>> paginationMono(Integer current, Integer size, Boolean needCount) {
        return reactiveEntityTemplate.select(Order.class)
                .page(PageRequest.of(current, size).withSort(Sort.by(Sort.Direction.DESC, "id")));
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
        return orderService.insert(order);
    }

    @PostMapping("/order/save")
    public Mono<Order> save(@RequestBody Order order) {
        order.setNumber(Integer.toString(new Random().nextInt()));
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
        return orderService.selectById(id);
//        return r2dbcEntityTemplate.select(Order.class)
//                .matching(Query.query(Criteria.where("id").is(id)))
//                .one();
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
        return reactiveEntityTemplate.destroy(Order.class)
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
                .phone(Integer.toString(new Random().nextInt()))
                .build();
//        Mono<User> userMono = Mono.just();
        Order order = Order.builder()
                .number(Integer.toString(new Random().nextInt()))
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
