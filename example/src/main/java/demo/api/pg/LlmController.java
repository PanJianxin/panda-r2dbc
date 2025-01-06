package demo.api.pg;

import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.core.enhance.query.criteria.EnhancedCriteria;
import demo.model.pg.LlmModel;
import demo.model.pg.LlmPrompt;
import lombok.RequiredArgsConstructor;
import org.springframework.data.relational.core.query.Query;
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
    public Mono<List<LlmPrompt>> listById() {
        List<Long> ids = List.of(3239241557395636225L, 3239266354066358273L);
        return reactiveEntityTemplate.select(LlmPrompt.class)
                .matching(Query.query(EnhancedCriteria.where(LlmPrompt::getRole).is(LlmPrompt.Role.SYSTEM)))
                .all().collectList();
    }

}
