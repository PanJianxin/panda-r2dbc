package demo.model;

import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableEntity;
import com.jxpanda.r2dbc.spring.data.extension.entity.StandardEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Panda
 */
@Data
@TableEntity(name = "sub_table")
@EqualsAndHashCode(callSuper = true)
public class SubTable extends StandardEntity<String> {

    @TableColumn(name = "content")
    private String content;

    @TableColumn(name = "main_table_id")
    private String mainTableId;

}