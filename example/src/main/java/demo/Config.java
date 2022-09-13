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

}
