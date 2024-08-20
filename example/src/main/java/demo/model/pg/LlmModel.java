package demo.model.pg;


import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

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
    private String clientType;

    @TableColumn(name = "client_options", isJson = true)
    private ClientOptions clientOptions;


    @Data
    public static class ClientOptions {
        private Double topP;
    }

}
