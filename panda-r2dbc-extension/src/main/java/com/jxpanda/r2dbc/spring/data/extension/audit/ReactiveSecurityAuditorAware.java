package com.jxpanda.r2dbc.spring.data.extension.audit;

import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

public class ReactiveSecurityAuditorAware implements ReactiveAuditorAware<String> {

    @NonNull
    @Override
    public Mono<String> getCurrentAuditor() {
        return Mono.just("");
    }

}
