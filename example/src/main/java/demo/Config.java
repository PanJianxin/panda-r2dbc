package demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jxpanda.commons.toolkit.json.JsonKit;
import com.jxpanda.r2dbc.spring.data.convert.MappingReactiveConverter;
import com.jxpanda.r2dbc.spring.data.core.ReactiveEntityTemplate;
import com.jxpanda.r2dbc.spring.data.extension.constant.StringConstant;
import com.jxpanda.r2dbc.spring.data.extension.support.IdGenerator;
import com.jxpanda.r2dbc.spring.data.extension.support.SnowflakeIdGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.mapping.DefaultNamingStrategy;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.r2dbc.core.DatabaseClient;


@Configuration
public class Config {

    @Bean
    public ObjectMapper objectMapper() {
        return JsonKit.jackson();
    }

}
