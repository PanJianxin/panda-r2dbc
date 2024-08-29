package demo.api.pg;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import demo.model.pg.LlmModel;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

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

    @GetMapping("list")
    public Mono<List<LlmModel>> listById() {
        List<Long> ids = List.of(3239241557395636225L, 3239266354066358273L);
        return reactiveEntityTemplate.select(LlmModel.class)
                .byIds(ids).collectList();
    }

}
