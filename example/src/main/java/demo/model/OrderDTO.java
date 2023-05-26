package demo.model;

import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableId;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableJoin;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableJoin(leftTable = "order", rightTable = "order_item", on = "`order`.id = order_item.order_id")
public class OrderDTO {

    @TableId
    @TableColumn(name = "id")
    private String id;

    @TableColumn(name = "parent_id")
    private String parentId;

    @TableColumn(name = "store_id")
    private String storeId;

    @TableColumn(name = "number")
    private String number;

    @TableColumn(name = "device")
    private Order.Device device;

    @TableColumn(name = "seller_id")
    private String sellerId;

    @TableColumn(name = "user_id")
    private String userId;

    @TableColumn(name = "phone")
    private String phone;

    @TableColumn(name = "type")
    private Order.Type type;

    @TableColumn(name = "status")
    private Order.Status status;

    @TableColumn(name = "payment_date")
    private LocalDateTime paymentDate;

    @TableColumn(name = "finish_date")
    private LocalDateTime finishDate;

    @TableColumn(name = "cost_amount")
    private BigDecimal costAmount;

    @TableColumn(name = "original_amount")
    private BigDecimal originalAmount;

    @TableColumn(name = "discount_amount")
    private BigDecimal discountAmount;

    @TableColumn(name = "amount")
    private BigDecimal amount;

    @TableColumn(name = "remark")
    private String remark;

    @TableColumn(name = "extend", isJson = true)
    private List<Order.Extend> extend;

    @TableColumn(name = "amount_changes", isJson = true)
    private List<Order.AmountChange> amountChange;

    @TableColumn(name = "id", fromTable = "order_item", alias = "itemId")
    private String itemId;

    @TableColumn(name = "order_id", fromTable = "order_item")
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
