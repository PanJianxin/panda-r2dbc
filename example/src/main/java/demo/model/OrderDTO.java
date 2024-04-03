package demo.model;

import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableId;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableJoin;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Panda
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableJoin(leftTable = "order", rightTable = "order_item", on = "`order`.id = order_item.order_id")
public class OrderDTO extends Order {

    @TableColumn(name = "id", alias = "itemId", fromTable = "order_item")
    private String itemId;

    @TableColumn(name = "order_id", alias = "order_id", fromTable = "order_item")
    private String orderId;

    @TableColumn(name = "sku_id", fromTable = "order_item")
    private String skuId;

    @TableColumn(name = "name", fromTable = "order_item")
    private String name;

    @TableColumn(name = "main_image", fromTable = "order_item")
    private String mainImage;

    @TableColumn(name = "introduction", fromTable = "order_item")
    private String introduction;

    @TableColumn(name = "specs", isJson = true, fromTable = "order_item")
    private List<Object> specs;

    @TableColumn(name = "price", fromTable = "order_item")
    private BigDecimal price;

    @TableColumn(name = "quantity", fromTable = "order_item")
    private Integer quantity;

}
