package com.hydrospark.billing.service;

import com.hydrospark.billing.model.*;
import com.hydrospark.billing.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingPeriodService {

    private final BillingPeriodRepository billingPeriodRepository;
    private final CustomerRepository customerRepository;
    private final MeterRepository meterRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final RatePlanRepository ratePlanRepository;
    private final BillRepository billRepository;
    private final BillLineItemRepository billLineItemRepository;
    private final RateEngineService rateEngineService;
    private final EmailService emailService;

    /**
     * Generate a new billing period for a specific cycle
     */
    @Transactional
    public BillingPeriod generateBillingPeriod(int cycleNumber, LocalDate periodStart, LocalDate periodEnd) {
        log.info("Generating billing period for cycle {} from {} to {}", cycleNumber, periodStart, periodEnd);

        // Validate dates
        if (periodStart.isAfter(periodEnd)) {
            throw new RuntimeException("Period start date must be before end date");
        }

        // Check for overlapping periods
        var existing = billingPeriodRepository.findByCycleNumberAndPeriodStartDate(cycleNumber, periodStart);
        if (existing.isPresent()) {
            throw new RuntimeException("Billing period already exists for this cycle and start date");
        }

        // Create billing period
        BillingPeriod period = BillingPeriod.builder()
                .cycleNumber(cycleNumber)
                .periodStartDate(periodStart)
                .periodEndDate(periodEnd)
                .status(BillingPeriod.Status.OPEN)
                .build();

        period = billingPeriodRepository.save(period);

        log.info("Billing period created: {}", period.getId());
        return period;
    }

    /**
     * Generate billing period for current month based on cycle number
     */
    @Transactional
    public BillingPeriod generateMonthlyPeriod(int cycleNumber) {
        LocalDate today = LocalDate.now();
        LocalDate periodStart = today.withDayOfMonth(1);
        LocalDate periodEnd = today.with(TemporalAdjusters.lastDayOfMonth());

        return generateBillingPeriod(cycleNumber, periodStart, periodEnd);
    }

    /**
     * Run billing for a period - generate all customer bills
     */
    @Transactional
    public BillingRunResult runBillingForPeriod(String periodId) {
        log.info("Starting billing run for period: {}", periodId);

        BillingPeriod period = billingPeriodRepository.findById(periodId)
                .orElseThrow(() -> new RuntimeException("Billing period not found"));

        if (period.getStatus() == BillingPeriod.Status.BILLED) {
            throw new RuntimeException("Billing has already been run for this period");
        }

        // Get all customers for this cycle
        List<Customer> customers = customerRepository.findByBillingCycleNumber(period.getCycleNumber());
        log.info("Found {} customers for cycle {}", customers.size(), period.getCycleNumber());

        BillingRunResult result = new BillingRunResult();
        result.setPeriodId(periodId);
        result.setTotalCustomers(customers.size());

        BigDecimal totalBilled = BigDecimal.ZERO;

        // Generate bills for each customer
        for (Customer customer : customers) {
            try {
                Bill bill = generateBillForCustomer(customer, period);
                if (bill != null) {
                    result.incrementSuccess();
                    totalBilled = totalBilled.add(bill.getTotalAmount());
                }
            } catch (Exception e) {
                log.error("Error generating bill for customer {}: {}", customer.getId(), e.getMessage(), e);
                result.incrementFailed();
                result.addError(customer.getId(), e.getMessage());
            }
        }

        // Update period status
        period.setStatus(BillingPeriod.Status.BILLED);
        billingPeriodRepository.save(period);

        result.setTotalAmount(totalBilled);
        result.setCompletedAt(LocalDateTime.now());

        log.info("Billing run complete: {} successful, {} failed, total: ${}",
                result.getSuccessCount(), result.getFailedCount(), totalBilled);

        return result;
    }

    /**
     * Generate a bill for a single customer
     */
    @Transactional
    public Bill generateBillForCustomer(Customer customer, BillingPeriod period) {
        log.debug("Generating bill for customer: {}", customer.getId());

        // Check if bill already exists
        var existingBill = billRepository.findByCustomerIdAndBillingPeriodId(
                customer.getId(), period.getId());
        if (existingBill.isPresent()) {
            log.debug("Bill already exists for customer {} in period {}", customer.getId(), period.getId());
            return existingBill.get();
        }

        // Get customer's active meters
        List<Meter> meters = meterRepository.findByCustomerId(customer.getId()).stream()
                .filter(m -> m.getStatus() == Meter.Status.ACTIVE)
                .toList();

        if (meters.isEmpty()) {
            log.warn("No active meters found for customer {}", customer.getId());
            return null;
        }

        // Calculate total usage across all meters
        BigDecimal totalUsageCcf = BigDecimal.ZERO;
        for (Meter meter : meters) {
            BigDecimal meterUsage = meterReadingRepository.sumUsageByMeterAndDateRange(
                    meter.getId(),
                    period.getPeriodStartDate(),
                    period.getPeriodEndDate()
            );
            if (meterUsage != null) {
                totalUsageCcf = totalUsageCcf.add(meterUsage);
            }
        }

        // Get active rate plan for customer type
        RatePlan.CustomerTypeScope customerTypeScope = customer.getCustomerType() == Customer.CustomerType.RESIDENTIAL
                ? RatePlan.CustomerTypeScope.RESIDENTIAL
                : RatePlan.CustomerTypeScope.COMMERCIAL;

        RatePlan ratePlan = ratePlanRepository.findActiveRatePlanForCustomerType(
                customerTypeScope, period.getPeriodEndDate())
                .orElseThrow(() -> new RuntimeException("No active rate plan found for customer type: " + customerTypeScope));

        // Calculate charges using rate engine
        RateEngineService.ChargeBreakdown charges = rateEngineService.calculateCharges(
                totalUsageCcf, ratePlan, period.getPeriodEndDate());

        // Create bill
        Bill bill = Bill.builder()
                .customerId(customer.getId())
                .meterId(meters.get(0).getId()) // Use first meter for reference
                .billingPeriodId(period.getId())
                .issueDate(LocalDate.now())
                .dueDate(period.getPeriodEndDate().plusDays(30)) // 30 days to pay
                .status(Bill.Status.DRAFT)
                .subtotal(charges.getSubtotal())
                .totalFees(charges.getBaseFee())
                .totalSurcharges(charges.getTotalSurcharges())
                .totalAmount(charges.getTotalAmount())
                .build();

        bill = billRepository.save(bill);

        // Create line items from charge breakdown
        createLineItems(bill, charges);

        log.debug("Bill generated for customer {}: ${}", customer.getId(), charges.getTotalAmount());

        return bill;
    }

    /**
     * Create bill line items from charge breakdown
     */
    private void createLineItems(Bill bill, RateEngineService.ChargeBreakdown charges) {
        List<BillLineItem> lineItems = new ArrayList<>();

        for (RateEngineService.ChargeBreakdown.LineItem chargeItem : charges.getLineItems()) {
            BillLineItem lineItem = BillLineItem.builder()
                    .billId(bill.getId())
                    .lineType(mapCategoryToLineType(chargeItem.getCategory()))
                    .description(chargeItem.getDescription())
                    .amount(chargeItem.getAmount())
                    .build();

            lineItems.add(lineItem);
        }

        billLineItemRepository.saveAll(lineItems);
    }

    private BillLineItem.LineType mapCategoryToLineType(String category) {
        return switch (category.toUpperCase()) {
            case "USAGE CHARGE", "USAGE" -> BillLineItem.LineType.USAGE_CHARGE;
            case "FIXED FEE", "FEE" -> BillLineItem.LineType.FEE;
            case "SURCHARGE" -> BillLineItem.LineType.SURCHARGE;
            default -> BillLineItem.LineType.USAGE_CHARGE;
        };
    }

    /**
     * Issue all bills for a period (mark as ISSUED)
     */
    @Transactional
    public void issueBillsForPeriod(String periodId) {
        log.info("Issuing bills for period: {}", periodId);

        BillingPeriod period = billingPeriodRepository.findById(periodId)
                .orElseThrow(() -> new RuntimeException("Billing period not found"));

        List<Bill> bills = billRepository.findByBillingPeriodId(periodId);

        int issued = 0;
        for (Bill bill : bills) {
            if (bill.getStatus() == Bill.Status.DRAFT) {
                bill.setStatus(Bill.Status.ISSUED);
                billRepository.save(bill);
                issued++;
            }
        }

        log.info("Issued {} bills for period {}", issued, periodId);
    }

    /**
     * Send bills via email for a period
     */
    @Transactional
    public SendBillsResult sendBillsForPeriod(String periodId) {
        log.info("Sending bills for period: {}", periodId);

        BillingPeriod period = billingPeriodRepository.findById(periodId)
                .orElseThrow(() -> new RuntimeException("Billing period not found"));

        List<Bill> bills = billRepository.findByBillingPeriodId(periodId).stream()
                .filter(b -> b.getStatus() == Bill.Status.ISSUED)
                .toList();

        SendBillsResult result = new SendBillsResult();
        result.setTotalBills(bills.size());

        for (Bill bill : bills) {
            try {
                emailService.sendBillEmail(bill);
                bill.setStatus(Bill.Status.SENT);
                bill.setDeliveredVia(Bill.DeliveryMethod.EMAIL);
                billRepository.save(bill);
                result.incrementSent();
            } catch (Exception e) {
                log.error("Failed to send bill {}: {}", bill.getId(), e.getMessage());
                result.incrementFailed();
            }
        }

        log.info("Sent {} of {} bills for period {}", result.getSentCount(), bills.size(), periodId);

        return result;
    }

    /**
     * Get all billing periods
     */
    public List<BillingPeriod> getAllPeriods() {
        return billingPeriodRepository.findAll();
    }

    /**
     * Get billing periods by cycle
     */
    public List<BillingPeriod> getPeriodsByCycle(int cycleNumber) {
        return billingPeriodRepository.findByCycleNumber(cycleNumber);
    }

    /**
     * Get billing period by ID
     */
    public BillingPeriod getPeriodById(String periodId) {
        return billingPeriodRepository.findById(periodId)
                .orElseThrow(() -> new RuntimeException("Billing period not found"));
    }

    // Result classes
    public static class BillingRunResult {
        private String periodId;
        private int totalCustomers;
        private int successCount;
        private int failedCount;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private LocalDateTime completedAt;
        private List<BillingError> errors = new ArrayList<>();

        public void incrementSuccess() { successCount++; }
        public void incrementFailed() { failedCount++; }
        public void addError(String customerId, String message) {
            errors.add(new BillingError(customerId, message));
        }

        // Getters and setters
        public String getPeriodId() { return periodId; }
        public void setPeriodId(String periodId) { this.periodId = periodId; }
        public int getTotalCustomers() { return totalCustomers; }
        public void setTotalCustomers(int totalCustomers) { this.totalCustomers = totalCustomers; }
        public int getSuccessCount() { return successCount; }
        public int getFailedCount() { return failedCount; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
        public List<BillingError> getErrors() { return errors; }

        public static class BillingError {
            private String customerId;
            private String message;

            public BillingError(String customerId, String message) {
                this.customerId = customerId;
                this.message = message;
            }

            public String getCustomerId() { return customerId; }
            public String getMessage() { return message; }
        }
    }

    public static class SendBillsResult {
        private int totalBills;
        private int sentCount;
        private int failedCount;

        public void incrementSent() { sentCount++; }
        public void incrementFailed() { failedCount++; }

        public int getTotalBills() { return totalBills; }
        public void setTotalBills(int totalBills) { this.totalBills = totalBills; }
        public int getSentCount() { return sentCount; }
        public int getFailedCount() { return failedCount; }
    }
}
