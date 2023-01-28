package demo.model;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.extension.service.ServiceImpl;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class OrderService extends ServiceImpl<Order, String> {

    public OrderService(ReactiveEntityTemplate reactiveEntityTemplate) {
        super(reactiveEntityTemplate);
    }

    @Override
    public Mono<Order> selectById(String s) {
        return super.selectById(s);
    }

    @Override
    public Mono<Order> insert(Order entity) {
        entity.setNumber(getIdGenerator().generate());
        return super.insert(entity);
    }

    public Mono<Order> selectByNumber(String number) {
        return getRepository(OrderRepository.class).findByNumber(number);
    }

//    @Override
//    protected OrderRepository getRepository() {
//        return super.getRepository();
//    }
}
