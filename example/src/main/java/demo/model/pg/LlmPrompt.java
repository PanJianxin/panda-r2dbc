package demo.model.pg;

import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * @author Panda
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableEntity(name = "llm_prompt",schema = "ai")
@EqualsAndHashCode(callSuper = true)
public class LlmPrompt extends Entity {

    @TableColumn(name = "name")
    private String name;

    @TableColumn(name = "content")
    private String content;

    @TableColumn(name = "version")
    private String version;

    @TableColumn(name = "params")
    private List<Param> params;

    @TableColumn(name = "is_enable")
    private boolean enable;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Param {
        private String name;
        private String script;
        private String scriptReturnType;
    }

}
