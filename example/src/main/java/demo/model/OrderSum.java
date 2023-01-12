package demo.model;

import com.jxpanda.r2dbc.spring.data.extension.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.extension.annotation.TableEntity;
import lombok.Data;

@Data
@TableEntity(name = "order", isAggregate = true)
public class OrderSum {

    @TableColumn(name = "*", alias = "count", function = "count")
    private Integer count;

    @TableColumn(name = "sum(amount)", alias = "total_amount")
    private Integer totalAmount;

}
