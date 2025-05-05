package demo.model.join;

import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableColumn;
import com.jxpanda.r2dbc.spring.data.core.enhance.annotation.TableJoin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableJoin(leftTable = "invoice_info", rightTable = "project_info", on = "invoice_info.project_code=project_info.project_code", joinType = TableJoin.JoinType.LEFT_JOIN)
public class InvoiceInfoCheck {

    @TableColumn(name = "id", fromTable = "invoice_info")
    private String invoiceInfoId;

    @TableColumn(name = "project_code", fromTable = "invoice_info")
    private String projectCode;

    @TableColumn(name = "project_name", fromTable = "invoice_info")
    private String projectName;

    @TableColumn(name = "buyer_name", fromTable = "invoice_info")
    private String buyerName;

    @TableColumn(name = "invoice_date", fromTable = "invoice_info")
    private LocalDateTime invoiceDate;

    @TableColumn(name = "goods_name", fromTable = "invoice_info")
    private String goodsName;

    @TableColumn(name = "total_amount_with_tax", fromTable = "invoice_info")
    private BigDecimal totalAmountWithTax;

    /**
     * 以下字段来自ProjectInfo表
     **/
    @TableColumn(name = "id", fromTable = "project_info", alias = "project_info_id")
    private String projectInfoId;

    @TableColumn(name = "contract_establish_date", fromTable = "project_info")
    private LocalDateTime contractEstablishDate;

    @TableColumn(name = "contract_number", fromTable = "project_info")
    private String contractNumber;

    @TableColumn(name = "client_name", fromTable = "project_info")
    private String clientName;

    @TableColumn(name = "project_settlement_date", fromTable = "project_info")
    private LocalDateTime projectSettlementDate;

    @TableColumn(name = "project_payment_date", fromTable = "project_info")
    private LocalDateTime projectPaymentDate;

    @TableColumn(name = "agency_fee", fromTable = "project_info")
    private BigDecimal agencyFee;

    @TableColumn(name = "document_fee", fromTable = "project_info")
    private BigDecimal documentFee;

    @TableColumn(name = "applicants", fromTable = "project_info")
    private String applicants;

}

