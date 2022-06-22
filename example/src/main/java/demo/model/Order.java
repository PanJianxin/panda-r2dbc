package demo.model;


import demo.EnumValue;
import demo.TableColumn;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


/**
 * 订单
 *
 * @author Panda
 * @since 2021-12-13
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Table(value = "`order`")
public class Order extends Entity {

    private static final long serialVersionUID = 1L;

    @TableColumn("`parent_id`")
    private String parentId;

    @TableColumn("`store_id`")
    private String storeId;

    @TableColumn("`number`")
    private String number;

    @TableColumn("`device`")
    private Device device;

    @TableColumn("`seller_id`")
    private String sellerId;

    @TableColumn("`user_id`")
    private String userId;

    @TableColumn("`phone`")
    private String phone;

    @TableColumn("`type`")
    private Type type;

    @TableColumn("`status`")
    private Status status;

    @TableColumn("`payment_date`")
    private LocalDateTime paymentDate;

    @TableColumn("`finish_date`")
    private LocalDateTime finishDate;

    @TableColumn("`cost_amount`")
    private BigDecimal costAmount;

    @TableColumn("`original_amount`")
    private BigDecimal originalAmount;

    @TableColumn("`discount_amount`")
    private BigDecimal discountAmount;

    @TableColumn("`amount`")
    private BigDecimal amount;

    @TableColumn("`remark`")
    private String remark;

    @TableColumn(value = "`extend`", isJson = true)
    private List<Extend> extend;

    @TableColumn(value = "`amount_changes`", isJson = true)
    private List<AmountChange> amountChange;

    @Getter
    @AllArgsConstructor
    public enum Device {
        /**
         * 未知
         */
        UNKNOWN(0, "未知"),
        /**
         * 安卓
         */
        ANDROID(1, "安卓"),
        /**
         * 微信小程序
         */
        WECHAT(3, "微信小程序"),
        /**
         * 后台加单
         */
        MANAGER(4, "后台加单");

        @EnumValue
        private final Integer code;
        private final String description;
    }

    @Getter
    @AllArgsConstructor
    public enum Type {
        /**
         * 未知
         */
        UNKNOWN(0, "未知"),
        /**
         * 普通订单
         */
        NORMAL(1, "普通订单"),
        /**
         * 堂食
         */
        HALL(2, "堂食"),
        /**
         * 预约
         */
        APPOINTMENT(3, "预约"),
        /**
         * 子订单
         */
        SUB_ORDER(127, "子订单");

        @EnumValue
        private final Integer code;
        private final String description;
    }

    @Getter
    @AllArgsConstructor
    public enum Status {
        /**
         * 未知
         */
        UNKNOWN(0, "未知"),
        /**
         * 待支付
         */
        WAITING_FOR_PAY(1, "待支付"),
        /**
         * 已支付
         */
        PAID(2, "已支付"),
        /**
         * 已发货
         */
        DELIVERED(3, "已发货"),
        /**
         * 已完成
         */
        FINISHED(4, "已完成"),
        /**
         * 已关闭
         */
        CLOSED(5, "已关闭"),
        /**
         * 待退款
         */
        WAITING_FOR_REFUND(6, "待退款"),
        /**
         * 已退款
         */
        REFUNDED(7, "已退款");

        @EnumValue
        private final Integer code;
        private final String description;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class Extend {
        private String key;
        private String value;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class AmountChange {
        private String bizId;
        private String bizType;
        private Integer rank;
        private String reason;
        private BigDecimal value;
    }

    public static final String PARENT_ID = "`parent_id`";
    public static final String STORE_ID = "`store_id`";
    public static final String NUMBER = "`number`";
    public static final String DEVICE = "`device`";
    public static final String SELLER_ID = "`seller_id`";
    public static final String USER_ID = "`user_id`";
    public static final String PHONE = "`phone`";
    public static final String TYPE = "`type`";
    public static final String STATUS = "`status`";
    public static final String PAYMENT_DATE = "`payment_date`";
    public static final String FINISH_DATE = "`finish_date`";
    public static final String COST_AMOUNT = "`cost_amount`";
    public static final String ORIGINAL_AMOUNT = "`original_amount`";
    public static final String DISCOUNT_AMOUNT = "`discount_amount`";
    public static final String AMOUNT = "`amount`";
    public static final String REMARK = "`remark`";
    public static final String EXTEND = "`extend`";
    public static final String AMOUNT_CHANGES = "`amount_changes`";

}
