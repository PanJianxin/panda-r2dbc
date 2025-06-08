package demo;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.extension.service.DefaultReactiveEntityRepository;
import demo.model.Test2;
import org.springframework.stereotype.Service;

@Service
public class Test2Repository extends DefaultReactiveEntityRepository<Test2> {
    public Test2Repository(ReactiveEntityTemplate reactiveEntityTemplate) {
        super(reactiveEntityTemplate, Test2.class);
    }
}
