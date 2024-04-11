package demo.model;

import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableEntity;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableEntity(name = "order", aggregate = true)
public class OrderSum {

    @TableColumn(name = "count(*)")
    private Integer count;

    @TableColumn(name = "amount", function = "sum", alias = "total_amount")
    private BigDecimal totalAmount;

}
