package demo.model;

import com.jxpanda.r2dbc.spring.data.mapping.annotation.TableColumn;
import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("`order`")
public class OrderSum {

    @TableColumn(value = "count(*)")
    private Integer count;

    @TableColumn(value = "sum(amount)")
    private Integer totalAmount;

}
