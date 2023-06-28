package demo.covert;

import com.jxpanda.r2dbc.spring.data.config.R2dbcConfigProperties;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;

public class DeleteDateTimeHandler implements R2dbcConfigProperties.LogicDelete.ValueHandler {

    @Override
    @NonNull
    public Object covert(@NonNull String value) {
        if (value.equals("CURRENT_TIMESTAMP")) {
            return LocalDateTime.now();
        }
        return value;
    }
}
