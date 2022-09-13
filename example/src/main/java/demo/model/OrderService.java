package demo.model;

import com.jxpanda.r2dbc.spring.data.core.R2dbcEntityTemplate;
import com.jxpanda.r2dbc.spring.data.extension.service.ServiceImpl;
import com.jxpanda.r2dbc.spring.data.extension.service.ServiceRepository;
import com.jxpanda.r2dbc.spring.data.repository.R2dbcRepository;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.repository.support.MappingRelationalEntityInformation;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class OrderService extends ServiceImpl<String, Order> {

//    public OrderService(R2dbcEntityTemplate r2dbcEntityTemplate) {
//        super(r2dbcEntityTemplate);
//    }

    public OrderService(R2dbcEntityTemplate r2dbcEntityTemplate, OrderRepository repository) {
        super(r2dbcEntityTemplate, repository);
    }

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

    @Override
    public Class<Order> getEntityClass() {
        return Order.class;
    }

    @Override
    protected Class<String> getIdClass() {
        return String.class;
    }
}
