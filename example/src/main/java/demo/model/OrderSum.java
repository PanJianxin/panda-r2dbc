package demo.model;

import com.jxpanda.r2dbc.spring.data.extension.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.extension.annotation.TableEntity;
import com.jxpanda.r2dbc.spring.data.extension.annotation.TableId;
import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

@Data
@TableEntity(name = "order", isQuery = true)
public class OrderSum {

    @TableColumn(name = "*", alias = "count", function = "count")
    private Integer count;

    @TableColumn(name = "sum(amount)", alias = "total_amount")
    private Integer totalAmount;

}
