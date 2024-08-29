package demo.model.pg;


import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Map;
import java.util.function.Function;

/**
 * @author Panda
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableEntity(name = "llm_model", schema = "ai")
@EqualsAndHashCode(callSuper = true)
public class LlmModel extends Entity {

    @TableColumn(name = "platform_name")
    private String platformName;

    @TableColumn(name = "model")
    private String model;

    @TableColumn(name = "url")
    private String url;

    @TableColumn(name = "api_key")
    private String apiKey;

    @TableColumn(name = "is_compatible_open_ai")
    private boolean compatibleOpenAi;

    @TableColumn(name = "is_enable")
    private boolean enable;

    @TableColumn(name = "client_type")
    private LlmClientType clientType;

    @TableColumn(name = "client_options", isJson = true)
    private Map<String, Object> clientOptions;


    /**
     * 语言模型客户端名称枚举。
     * 该枚举定义了可用的语言模型客户端及其对应的客户端类。
     *
     * @author Panda
     */
    @RequiredArgsConstructor
    public enum LlmClientType {

        /**
         * 未知客户端类型。
         * 使用"unknown"作为标识符，并指定UnknownClient类作为其实现。
         */
        UNKNOWN("unknown", null),

        /**
         * REST客户端。
         * 使用"rest"作为标识符，并指定RestClient类作为其实现。
         */
        REST("rest 客户端", null),

        /**
         * 开放人工智能客户端。
         * 使用"openai"作为标识符，并指定OpenAIClient类作为其实现。
         */
        OPEN_AI("openai 客户端", null),

        /**
         * 达摩院客户端。
         * 使用"dashscope"作为标识符，并指定DashScopeClient类作为其实现。
         */
        DASH_SCOPE("dashscope 阿里云百炼客户端", null);


        /**
         * 客户端名称。
         * 每个客户端枚举值都具有唯一的名称。
         */
        @Getter
        private final String description;

        /**
         * 客户端类。
         * 每个客户端枚举值对应一个实现了LlmClient接口的类。
         * 这允许通过枚举值动态创建和使用客户端实例。
         * 这个函数只允许同包的LlmClient调用，避免通过其他途经错误的初始化客户端的可能性
         */
        @Getter(value = AccessLevel.PACKAGE)
        private final Function<LlmModel, Object> client;


    }


}
