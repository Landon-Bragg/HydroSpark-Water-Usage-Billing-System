package com.hydrospark.billing.controller;

import com.hydrospark.billing.model.Bill;
import com.hydrospark.billing.model.BillingPeriod;
import com.hydrospark.billing.service.BillingPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for billing period and bill generation operations
 */
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingPeriodService billingPeriodService;

    /**
     * Get all billing periods
     */
    @GetMapping("/periods")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING')")
    public ResponseEntity<List<BillingPeriod>> getAllPeriods() {
        return ResponseEntity.ok(billingPeriodService.getAllPeriods());
    }

    /**
     * Get billing periods by cycle number
     */
    @GetMapping("/periods/cycle/{cycleNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING')")
    public ResponseEntity<List<BillingPeriod>> getPeriodsByCycle(@PathVariable Integer cycleNumber) {
        return ResponseEntity.ok(billingPeriodService.getPeriodsByCycle(cycleNumber));
    }

    /**
     * Get a specific billing period
     */
    @GetMapping("/periods/{periodId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING')")
    public ResponseEntity<BillingPeriod> getPeriod(@PathVariable String periodId) {
        return ResponseEntity.ok(billingPeriodService.getPeriodById(periodId));
    }

    /**
     * Generate a new billing period
     */
    @PostMapping("/periods/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING')")
    public ResponseEntity<BillingPeriod> generatePeriod(@RequestBody GeneratePeriodRequest request) {
        BillingPeriod period = billingPeriodService.generateBillingPeriod(
                request.cycleNumber(),
                request.periodStart(),
                request.periodEnd()
        );
        return ResponseEntity.ok(period);
    }

    /**
     * Generate monthly billing period for current month
     */
    @PostMapping("/periods/generate-monthly/{cycleNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING')")
    public ResponseEntity<BillingPeriod> generateMonthlyPeriod(@PathVariable Integer cycleNumber) {
        BillingPeriod period = billingPeriodService.generateMonthlyPeriod(cycleNumber);
        return ResponseEntity.ok(period);
    }

    /**
     * Run billing for a period (generate all customer bills)
     */
    @PostMapping("/periods/{periodId}/run")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING')")
    public ResponseEntity<BillingPeriodService.BillingRunResult> runBilling(@PathVariable String periodId) {
        BillingPeriodService.BillingRunResult result = billingPeriodService.runBillingForPeriod(periodId);
        return ResponseEntity.ok(result);
    }

    /**
     * Issue bills for a period (mark as ISSUED)
     */
    @PostMapping("/periods/{periodId}/issue")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING')")
    public ResponseEntity<Void> issueBills(@PathVariable String periodId) {
        billingPeriodService.issueBillsForPeriod(periodId);
        return ResponseEntity.ok().build();
    }

    /**
     * Send bills via email for a period
     */
    @PostMapping("/periods/{periodId}/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING')")
    public ResponseEntity<BillingPeriodService.SendBillsResult> sendBills(@PathVariable String periodId) {
        BillingPeriodService.SendBillsResult result = billingPeriodService.sendBillsForPeriod(periodId);
        return ResponseEntity.ok(result);
    }

    /**
     * Request record for generating a billing period
     */
    public record GeneratePeriodRequest(
            Integer cycleNumber,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd
    ) {}
}
