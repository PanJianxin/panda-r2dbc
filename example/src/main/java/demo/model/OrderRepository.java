package demo.model;

import com.jxpanda.r2dbc.spring.data.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface OrderRepository extends R2dbcRepository<Order, String> {
    Mono<Order> findByNumber(String number);

}
