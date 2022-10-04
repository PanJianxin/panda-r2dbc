package demo.model;

import com.jxpanda.r2dbc.spring.data.extension.annotation.TableColumn;
import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("order")
public class OrderSum {

    @TableColumn(name = "count(*)", alias = "count")
    private Integer count;

    @TableColumn(name = "sum(amount)", alias = "total_amount")
    private Integer totalAmount;

}
