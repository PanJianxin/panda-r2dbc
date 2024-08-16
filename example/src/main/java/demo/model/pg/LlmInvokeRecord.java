package demo.model.pg;

import com.jxpanda.r2dbc.spring.data.core.enhance.StandardEnum;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.EnumValue;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * @author Panda
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableEntity(name = "llm_invoke_record",schema = "ai")
@EqualsAndHashCode(callSuper = true)
public class LlmInvokeRecord extends Entity {

    @TableColumn(name = "user_content")
    private String userContent;

    @TableColumn(name = "llm_response")
    private String llmResponse;

    @TableColumn(name = "confirm_response")
    private String confirmResponse;

    @TableColumn(name = "is_response_equivalent")
    private Boolean responseEquivalent;

    @TableColumn(name = "prompt_id")
    private String promptId;

    @TableColumn(name = "prompt_name")
    private String promptName;

    @TableColumn(name = "llm_model_id")
    private String llmModelId;

    @TableColumn(name = "llm_model_name")
    private String llmModelName;

    @TableColumn(name = "llm_function_id")
    private String llmFunctionId;

    @TableColumn(name = "llm_function_name")
    private String llmFunctionName;

    @TableColumn(name = "confirm_status")
    private ConfirmStatus confirmStatus;

    @TableColumn(name = "confirm_id")
    private String confirmId;

    @TableColumn(name = "confirm_name")
    private String confirmName;

    @TableColumn(name = "updater_name")
    private String updaterName;


    @Getter
    @RequiredArgsConstructor
    public enum ConfirmStatus implements StandardEnum {
        /**
         * 未知
         */
        UNKNOWN(0, "未知"),
        /**
         * 记账
         */
        UNCONFIRMED(1, "未确认"),
        /**
         * 已付款
         */
        CONFIRMED(2, "已确认"),
        /**
         * 大模型自动修复
         */
        LLM_CORRECTED(3, "大模型自动修复"),
        /**
         * 已更正
         */
        USER_CORRECTED(4, "用户手动修复");

        @EnumValue
        private final Integer code;
        private final String description;
    }

}
