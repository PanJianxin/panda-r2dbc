package demo.model.pg;

import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableEntity;
import com.jxpanda.r2dbc.spring.data.infrastructure.kit.DateTimeKit;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Function;

/**
 * @author Panda
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableEntity(name = "llm_prompt", schema = "ai")
@EqualsAndHashCode(callSuper = true)
public class LlmPrompt extends Entity {

    @TableColumn(name = "name")
    private String name;

    @TableColumn(name = "content")
    private String content;

    @TableColumn(name = "version")
    private String version;

    @TableColumn(name = "role")
    private Role role;

    @TableColumn(name = "params", isJson = true)
    private List<Param> params;

    @TableColumn(name = "is_enable")
    private boolean enable;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Param {
        private String name;
        private String script;
        private ScriptReturnType scriptReturnType;
    }

    public enum Role{
        UNKNOWN,
        SYSTEM,
        USER,
        ASSISTANT;
    }

    @RequiredArgsConstructor
    public enum ScriptReturnType {
        /**
         * 字符串
         */
        STRING("string", String.class, Object::toString),
        /**
         * 整型
         */
        INTEGER("integer", Integer.class, o -> Integer.toString((int) o)),
        /**
         * 长整型
         */
        LONG("long", Long.class, o -> Long.toString((long) o)),
        /**
         * 浮点型
         */
        DOUBLE("double", Double.class, o -> Double.toString((double) o)),
        /**
         * 布尔型
         */
        BOOLEAN("boolean", Boolean.class, o -> Boolean.toString((boolean) o)),
        /**
         * 日期
         */
        DATE("date", LocalDate.class, o -> DateTimeKit.format((LocalDate) o)),
        /**
         * 时间
         */
        TIME("time", LocalTime.class, o -> DateTimeKit.format((LocalTime) o)),
        /**
         * 日期时间
         */
        DATE_TIME("date_time", LocalDateTime.class, o -> DateTimeKit.format((LocalDateTime) o));

        @Getter
        private final String name;
        private final Class<?> clazz;
        private final Function<Object, String> formatter;

        public String invokeScript(String jsCode) {
//            try (Context context = Context.create()) {
//                Value result = context.eval("js", jsCode);
//                return formatter.apply(result.as(clazz));
//            }
            return "";
        }

    }

}
