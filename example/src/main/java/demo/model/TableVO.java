package demo.model;

import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableEntity;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableJoin;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableReference;
import com.jxpanda.r2dbc.spring.data.extension.entity.StandardEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author Panda
 */
@Data
@TableEntity(name = "main_table")
@EqualsAndHashCode(callSuper = true)
@TableJoin(leftTable = "main_table", rightTable = "sub_table", on = "main_table.id = sub_table.main_table_id")
public class TableVO extends StandardEntity<String> {

    @TableColumn(name = "content")
    private String content;

    @TableColumn(name = "sub_table_ids", isJson = true)
    private String[] subTableIds;

    @TableColumn(name = "sub_table_ids", isJson = true)
    private List<String> subTableIdList;

    @TableColumn(name = "sub_table_ids_string")
    private String subTableIdsString;

    @TableColumn(name = "json_object", isJson = true)
    private Object jsonObject;

    @TableColumn(name = "sub_table.content", alias = "subContent")
    private String subContent;

    @TableColumn(name = "main_table_id", fromTable = "sub_table")
    private String mainTableId;

    @TableReference(keyColumn = "subTableIds", referenceColumn = "id")
    private SubTable[] subTables;

    @TableReference(keyColumn = "subTableIdsString", referenceColumn = "id", delimiter = ",", referenceCondition = TableReference.ReferenceCondition.IN)
    private List<SubTable> subTableList;

}
