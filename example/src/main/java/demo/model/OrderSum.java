package demo.model;

import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableEntity;
import lombok.Data;

@Data
@TableEntity(name = "order", aggregate = true)
public class OrderSum {

    @TableColumn(name = "*", alias = "count", function = "count")
    private Integer count;

    @TableColumn(name = "amount", alias = "total_amount", function = "sum")
    private Integer totalAmount;

//    @TableColumn(name = "amount", alias = "total_amount", function = "case when then")
//    private String test;

}
