package demo;

import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcAuditingPlugin;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcLogicDeletePlugin;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.R2dbcTenantPlugin;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model.R2dbcAuditingIdSupplier;
import com.jxpanda.r2dbc.spring.data.core.enhance.plugin.model.R2dbcTenantIdSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;


@Configuration
public class Config {

    @Bean
    public R2dbcTenantIdSupplier tenantIdSupplier() {
        return () -> Mono.just("1");
    }

    @Bean
    public R2dbcLogicDeletePlugin<?> logicDeletePlugin(){
        return new R2dbcLogicDeletePlugin<>();
    }

    @Bean
    public R2dbcTenantPlugin<?> tenantPlugin(R2dbcTenantIdSupplier tenantIdSupplier){
        return new R2dbcTenantPlugin<>(tenantIdSupplier);
    }

    @Bean
    public R2dbcAuditingPlugin<?> auditingPlugin(){
        return new R2dbcAuditingPlugin<>(new R2dbcAuditingIdSupplier() {
            @Override
            public Mono<String> getCreatorId() {
                return Mono.just("1");
            }

            @Override
            public Mono<String> getUpdaterId() {
                return Mono.just("2");
            }
        });
    }


}
