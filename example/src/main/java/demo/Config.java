package demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jxpanda.commons.toolkit.json.JsonKit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class Config {

    @Bean
    public ObjectMapper objectMapper() {
        return JsonKit.jackson();
    }

//    @NonNull
//    @Bean
//    public R2dbcConverter r2dbcConverter(R2dbcMappingContext mappingContext,
//                                         R2dbcCustomConversions r2dbcCustomConversions) {
//        return new MysqlR2dbcConverter(mappingContext, r2dbcCustomConversions);
//    }


    //    @NonNull
//    @Override
//    public ConnectionFactory connectionFactory() {
//        return MySqlConnectionFactory.from(MySqlConnectionConfiguration.builder()
//                .host("mysql.jxpanda.com")
//                .port(2333)
//                .username("Panda")
//                .password("Panda2022@)@@")
//                .database("hydrogen")
//                .build());
//    }
//
}
