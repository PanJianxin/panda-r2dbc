package demo.model.pg;

import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author Panda
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableEntity(name = "llm_function", schema = "ai")
@EqualsAndHashCode(callSuper = true)
public class LlmFunction extends Entity {

    @TableColumn(name = "name")
    private String name;

    @TableColumn(name = "prompt_id")
    private String promptId;

    @TableColumn(name = "prompt_name")
    private String promptName;

    @TableColumn(name = "model_id")
    private String modelId;

    @TableColumn(name = "model_name")
    private String modelName;

    @TableColumn(name = "is_current")
    private boolean current;


}
