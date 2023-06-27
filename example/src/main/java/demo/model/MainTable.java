package demo.model;


import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableEntity;
import com.jxpanda.r2dbc.spring.data.extension.entity.StandardEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * @author Panda
 */
@Data
@SuperBuilder
@TableEntity(name = "main_table")
@EqualsAndHashCode(callSuper = true)
public class MainTable extends StandardEntity<String> {

    @TableColumn(name = "content")
    private String content;

    @TableColumn(name = "sub_table_ids", isJson = true)
    private List<String> subTableIds;

    @TableColumn(name = "sub_table_ids_string")
    private String subTableIdsString;

    @TableColumn(name = "json_object", isJson = true)
    private Object jsonObject;

}
