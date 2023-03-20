package demo.model;

import com.jxpanda.r2dbc.spring.data.extension.service.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class OrderService extends ServiceImpl<Order, String> {

}
