package demo;

import org.springframework.context.annotation.Configuration;


@Configuration
public class Config {


//    @Bean
//    public ConnectionFactory connectionFactory(){
//
//        PostgresqlConnectionConfiguration config = PostgresqlConnectionConfiguration.builder()
//                .host("pgm-wz96l47sdu272fh6mo.rwlb.rds.aliyuncs.com")
//                .port(5432)
//                .database("floravita")
//                .username("floravita")
//                .password("Floravita2024@)@$")
//                .codecRegistrar(new CodecRegistrar() {
//
//                    @Override
//                    public Publisher<Void> register(PostgresqlConnection postgresqlConnection, ByteBufAllocator byteBufAllocator, CodecRegistry codecRegistry) {
//                        codecRegistry.addFirst(new PostgresDialect.JsonToStringCodec());
//                        return null;
//                    }
//                })
//                .build();
//
//        return new PostgresqlConnectionFactory(config);
//    }

}
