package com.jxpanda.autoconfigure.configure;

import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcAuditingPlugin;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcOperationPlugin;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcPluginExecutor;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model.R2dbcAuditingIdSupplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

import java.util.List;

public class PluginConfigure {

    @Bean
    @ConditionalOnMissingBean
    public R2dbcPluginExecutor r2dbcPluginExecutor(List<R2dbcOperationPlugin> plugins) {
        return new R2dbcPluginExecutor(plugins);
    }

    public R2dbcAuditingPlugin<?> auditingPlugin() {

        return new R2dbcAuditingPlugin<>(new R2dbcAuditingIdSupplier() {

            @Override
            public Mono<String> getCreatorId() {
                return null;
            }

            @Override
            public Mono<String> getUpdaterId() {
                return getPrincipal()
                        .map(principal -> {
                            return principal instanceof Jwt ? ((Jwt) principal).getClaimAsString("user_name") : principal.toString();
                        });
            }

            private Mono<Object> getPrincipal() {
                return ReactiveSecurityContextHolder.getContext()
                        .map(SecurityContext::getAuthentication)
                        .map(Authentication::getPrincipal);
            }
        });
    }

}
