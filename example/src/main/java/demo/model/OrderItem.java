package demo.model;

import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableEntity;
import com.jxpanda.r2dbc.spring.data.extension.entity.StandardEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableEntity(name = "order_item")
public class OrderItem extends StandardEntity<String> {

    @TableColumn(name = "order_id")
    private String orderId;

    @TableColumn(name = "user_id")
    private String userId;

    @TableColumn(name = "sku_id")
    private String skuId;

    @TableColumn(name = "name")
    private String name;

    @TableColumn(name = "main_image")
    private String mainImage;

    @TableColumn(name = "introduction")
    private String introduction;

    @TableColumn(name = "specs", isJson = true)
    private List<Object> specs;

    @TableColumn(name = "price")
    private BigDecimal price;

    @TableColumn(name = "quantity")
    private Integer quantity;

    public static final String ORDER_ID = "order_id";
    public static final String USER_ID = "user_id";
    public static final String SPU_ID = "spu_id";
    public static final String SKU_ID = "sku_id";
    public static final String SKU_SNAPSHOT_ID = "sku_snapshot_id";
    public static final String SPU_NAME = "spu_name";
    public static final String SKU_NAME = "sku_name";
    public static final String NAME = "name";
    public static final String MAIN_IMAGE = "main_image";
    public static final String INTRODUCTION = "introduction";
    public static final String SPECS = "specs";
    public static final String PRICE = "price";
    public static final String COST_PRICE = "cost_price";
    public static final String QUANTITY = "quantity";
    public static final String COST_AMOUNT = "cost_amount";
    public static final String ORIGINAL_AMOUNT = "original_amount";
    public static final String DISCOUNT_AMOUNT = "discount_amount";
    public static final String AMOUNT = "amount";


}
