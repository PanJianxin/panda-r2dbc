package demo.api.pg;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import demo.model.pg.LlmModel;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Panda
 */
@RestController
@RequestMapping("llm")
@RequiredArgsConstructor
public class LlmController implements Controller<LlmModel> {

    private final ReactiveEntityTemplate reactiveEntityTemplate;

    @Override
    public ReactiveEntityTemplate reactiveEntityTemplate() {
        return reactiveEntityTemplate;
    }
}
