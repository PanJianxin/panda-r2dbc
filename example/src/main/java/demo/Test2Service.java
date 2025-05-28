package demo;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.extension.service.DefaultReactiveEntityService;
import demo.model.Test2;
import org.springframework.stereotype.Service;

@Service
public class Test2Service extends DefaultReactiveEntityService<Test2> {
    public Test2Service(ReactiveEntityTemplate reactiveEntityTemplate) {
        super(reactiveEntityTemplate, Test2.class);
    }
}
