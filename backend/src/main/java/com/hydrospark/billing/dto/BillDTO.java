package com.hydrospark.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for bills.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillDTO {
    private String id;

    private String customerId;
    private String meterId;
    private String billingPeriodId;

    private LocalDate issueDate;
    private LocalDate dueDate;

    /** String representation of Bill.Status enum. */
    private String status;

    private BigDecimal subtotal;
    private BigDecimal totalFees;
    private BigDecimal totalSurcharges;
    private BigDecimal totalAmount;

    /** String representation of Bill.DeliveryMethod enum. */
    private String deliveredVia;

    private List<LineItemDTO> lineItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LineItemDTO {
        private String id;
        private String description;
        private BigDecimal amount;

        /** e.g., USAGE, FEE, SURCHARGE */
        private String category;
    }
}
