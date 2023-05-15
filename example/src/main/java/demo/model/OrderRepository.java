package demo.model;

import com.jxpanda.r2dbc.spring.data.extension.service.ServiceRepository;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface OrderRepository extends ServiceRepository<Order, String> {
    Mono<Order> findByNumber(String number);

    @Query("SELECT * FROM `order` WHERE id = :id")
    Mono<Order> selectBySql(String id);


}
