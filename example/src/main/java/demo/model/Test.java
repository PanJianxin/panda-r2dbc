package demo.model;

import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableEntity;
import com.jxpanda.r2dbc.spring.data.extension.entity.CrudEntity;
import com.jxpanda.r2dbc.spring.data.extension.entity.RichEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Data
@TableEntity(name = "test",ignoreColumns = {"acl_id"})
@NoArgsConstructor
@AllArgsConstructor
public class Test extends RichEntity implements CrudEntity {

    @TableColumn(name = "view_scope")
    private Integer viewScope;

}
