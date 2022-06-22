package demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jxpanda.commons.toolkit.json.JsonKit;
import com.jxpanda.r2dbc.spring.data.convert.MappingR2dbcConverter;
import com.jxpanda.r2dbc.spring.data.convert.R2dbcCustomConversions;
import com.jxpanda.r2dbc.spring.data.mapping.R2dbcMappingContext;
import demo.covert.MysqlR2dbcConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
public class Config   {

    @Bean
    public ObjectMapper objectMapper() {
        return JsonKit.jackson();
    }

//    @Bean
//    public R2dbcEntityTemplate r2dbcEntityTemplate(ConnectionFactory connectionFactory) {
//        return new R2dbcEntityTemplate(connectionFactory);
//    }


    @NonNull
    @Bean
    public MappingR2dbcConverter r2dbcConverter(R2dbcMappingContext mappingContext,
                                                R2dbcCustomConversions r2dbcCustomConversions) {
        return new MysqlR2dbcConverter(mappingContext, r2dbcCustomConversions);
    }


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
//    @NonNull
//    @Bean
//    public R2dbcCustomConversions r2dbcCustomConversions() {
//        List<Object> converterList = new ArrayList<>();
////        converterList.add(new OrderReadingCovert());
//        return new R2dbcCustomConversions(getStoreConversions(), converterList);
//    }
//

//    @NonNull
//    @Override
//    protected CustomConversions.StoreConversions getStoreConversions() {
//        List<Object> converters = new ArrayList<>();
//
////        converters.addAll(R2dbcConverters.getConvertersToRegister());
////        converters.addAll(JodaTimeConverters.getConvertersToRegister());
//
//        return CustomConversions.StoreConversions.of(R2dbcSimpleTypeHolder.HOLDER, Collections.unmodifiableList(converters));
//    }

//    @Override
//    protected List<Object> getCustomConverters() {
//        List<Object> converterList = new ArrayList<>();
//        converterList.add(new OrderReadingCovert());
//        return converterList;
//    }
}
