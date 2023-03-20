package demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jxpanda.r2dbc.spring.data.core.enhance.TableJoin;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableId;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableLogic;
import com.jxpanda.r2dbc.spring.data.extension.entity.Entity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableJoin
public class OrderVO implements Entity<String> {

    @TableId
    private String id;

    @TableColumn
    @JsonIgnore
    @TableLogic(undeleteValue = TableLogic.Value.DATETIME_1970, deleteValue = TableLogic.Value.DATETIME_NOW)
    private LocalDateTime deletedDate;

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

    @TableColumn(name = "items", isJson = true)
    private List<OrderItem> items;

}
