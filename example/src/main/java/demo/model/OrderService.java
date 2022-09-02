package demo.model;

import com.jxpanda.r2dbc.spring.data.core.R2dbcEntityTemplate;
import com.jxpanda.r2dbc.spring.data.extension.service.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class OrderService extends ServiceImpl<String,Order> {

    public OrderService(R2dbcEntityTemplate r2dbcEntityTemplate) {
        super(r2dbcEntityTemplate);
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
