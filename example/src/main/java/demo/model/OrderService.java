package demo.model;

import com.jxpanda.r2dbc.spring.data.extension.service.ServiceImpl;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class OrderService extends ServiceImpl<String, Order> {

    public OrderService(R2dbcEntityTemplate r2dbcEntityTemplate) {
        super(r2dbcEntityTemplate);
    }

//    public OrderService(R2dbcEntityTemplate r2dbcEntityTemplate, OrderRepository repository) {
//        super(r2dbcEntityTemplate, repository);
//    }

    @Override
    public Mono<Order> selectById(String s) {
        return super.selectById(s);
    }

    public Mono<Order> selectByNumber(String number){
        return getRepository().findByNumber(number);
    }

    @Override
    protected OrderRepository getRepository() {
        return super.getRepository();
    }

}
