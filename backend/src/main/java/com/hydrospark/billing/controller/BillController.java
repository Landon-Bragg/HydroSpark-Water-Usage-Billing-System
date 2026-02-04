package com.hydrospark.billing.controller;

import com.hydrospark.billing.dto.BillDTO;
import com.hydrospark.billing.model.Bill;
import com.hydrospark.billing.model.BillLineItem;
import com.hydrospark.billing.repository.BillLineItemRepository;
import com.hydrospark.billing.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for bill viewing and management
 */
@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillRepository billRepository;
    private final BillLineItemRepository billLineItemRepository;

    /**
     * Get all bills for a customer
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<BillDTO>> getCustomerBills(@PathVariable String customerId) {
        List<Bill> bills = billRepository.findByCustomerIdOrderByIssueDateDesc(customerId);

        List<BillDTO> dtos = bills.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get a specific bill by ID
     */
    @GetMapping("/{billId}")
    public ResponseEntity<BillDTO> getBillDetails(@PathVariable String billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        return ResponseEntity.ok(toDTO(bill));
    }

    /**
     * Get all bills for a billing period
     */
    @GetMapping("/period/{periodId}")
    public ResponseEntity<List<BillDTO>> getBillsByPeriod(@PathVariable String periodId) {
        List<Bill> bills = billRepository.findByBillingPeriodId(periodId);

        List<BillDTO> dtos = bills.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Record a payment for a bill
     */
    @PostMapping("/{billId}/pay")
    public ResponseEntity<BillDTO> recordPayment(
            @PathVariable String billId,
            @RequestBody PaymentRequest request) {

        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        if (bill.getStatus() == Bill.Status.PAID) {
            throw new RuntimeException("Bill has already been paid");
        }

        // Validate payment amount
        if (request.amount().compareTo(bill.getTotalAmount()) < 0) {
            throw new RuntimeException("Payment amount is less than bill total");
        }

        // Record payment
        bill.setStatus(Bill.Status.PAID);
        billRepository.save(bill);

        return ResponseEntity.ok(toDTO(bill));
    }

    /**
     * Get current (unpaid) bill for a customer
     */
    @GetMapping("/customer/{customerId}/current")
    public ResponseEntity<BillDTO> getCurrentBill(@PathVariable String customerId) {
        List<Bill> bills = billRepository.findByCustomerIdOrderByIssueDateDesc(customerId);

        // Find first unpaid bill
        Bill currentBill = bills.stream()
                .filter(b -> b.getStatus() != Bill.Status.PAID && b.getStatus() != Bill.Status.VOID)
                .findFirst()
                .orElse(null);

        if (currentBill == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toDTO(currentBill));
    }

    /**
     * Get overdue bills
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<BillDTO>> getOverdueBills() {
        LocalDate today = LocalDate.now();
        List<Bill.Status> unpaidStatuses = List.of(
                Bill.Status.ISSUED,
                Bill.Status.SENT
        );

        List<Bill> overdueBills = billRepository.findByDueDateBeforeAndStatusIn(today, unpaidStatuses);

        List<BillDTO> dtos = overdueBills.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Convert Bill entity to DTO
     */
    private BillDTO toDTO(Bill bill) {
        List<BillLineItem> lineItems = billLineItemRepository.findByBillId(bill.getId());

        List<BillDTO.LineItemDTO> lineItemDTOs = lineItems.stream()
                .map(li -> BillDTO.LineItemDTO.builder()
                        .id(li.getId())
                        .description(li.getDescription())
                        .amount(li.getAmount())
                        .category(li.getLineType() != null ? li.getLineType().name() : null)
                        .build())
                .collect(Collectors.toList());

        return BillDTO.builder()
                .id(bill.getId())
                .customerId(bill.getCustomerId())
                .meterId(bill.getMeterId())
                .billingPeriodId(bill.getBillingPeriodId())
                .issueDate(bill.getIssueDate())
                .dueDate(bill.getDueDate())
                .status(bill.getStatus() != null ? bill.getStatus().name() : null)
                .subtotal(bill.getSubtotal())
                .totalFees(bill.getTotalFees())
                .totalSurcharges(bill.getTotalSurcharges())
                .totalAmount(bill.getTotalAmount())
                .deliveredVia(bill.getDeliveredVia() != null ? bill.getDeliveredVia().name() : null)
                .lineItems(lineItemDTOs)
                .build();
    }

    /**
     * Payment request record
     */
    public record PaymentRequest(
            BigDecimal amount,
            String paymentMethod,
            String transactionId
    ) {}
}
