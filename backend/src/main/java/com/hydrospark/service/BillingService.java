package com.hydrospark.service;

import com.hydrospark.model.*;
import com.hydrospark.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final BillRepository billRepository;
    private final BillingPeriodRepository billingPeriodRepository;
    private final CustomerRepository customerRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final RatePlanRepository ratePlanRepository;
    private final RateEngineService rateEngineService;
    private final EmailService emailService;

    @Transactional
    public BillingPeriod generateBillingPeriod(LocalDate startDate, LocalDate endDate, LocalDate dueDate) {
        BillingPeriod period = new BillingPeriod();
        period.setId(UUID.randomUUID().toString());
        period.setPeriodName(startDate.getMonth() + " " + startDate.getYear());
        period.setStartDate(startDate);
        period.setEndDate(endDate);
        period.setDueDate(dueDate);
        period.setStatus("PENDING");
        
        return billingPeriodRepository.save(period);
    }

    @Transactional
    public void runBillingCycle(String billingPeriodId) {
        BillingPeriod period = billingPeriodRepository.findById(billingPeriodId)
                .orElseThrow(() -> new RuntimeException("Billing period not found"));

        period.setStatus("PROCESSING");
        billingPeriodRepository.save(period);

        List<Customer> customers = customerRepository.findAllByIsActive(true);
        RatePlan ratePlan = ratePlanRepository.findFirstByIsActiveTrue()
                .orElseThrow(() -> new RuntimeException("No active rate plan"));

        int billsGenerated = 0;
        BigDecimal totalBilled = BigDecimal.ZERO;

        for (Customer customer : customers) {
            try {
                Bill bill = generateBillForCustomer(customer, period, ratePlan);
                billsGenerated++;
                totalBilled = totalBilled.add(bill.getTotalAmount());
            } catch (Exception e) {
                // Log error but continue with other customers
                System.err.println("Error generating bill for customer " + customer.getId() + ": " + e.getMessage());
            }
        }

        period.setStatus("COMPLETED");
        period.setBillsGenerated(billsGenerated);
        period.setTotalBilled(totalBilled);
        billingPeriodRepository.save(period);
    }

    private Bill generateBillForCustomer(Customer customer, BillingPeriod period, RatePlan ratePlan) {
        // Calculate usage for the period
        BigDecimal totalUsage = meterReadingRepository
                .sumUsageByCustomerAndDateRange(customer.getId(), period.getStartDate(), period.getEndDate())
                .orElse(BigDecimal.ZERO);

        // Calculate charges using rate engine
        Map<String, BigDecimal> charges = rateEngineService.calculateCharges(totalUsage, ratePlan, period.getStartDate());

        // Create bill
        Bill bill = new Bill();
        bill.setId(UUID.randomUUID().toString());
        bill.setBillNumber("BILL-" + System.currentTimeMillis());
        bill.setCustomer(customer);
        bill.setBillingPeriod(period);
        bill.setRatePlan(ratePlan);
        bill.setIssueDate(LocalDate.now());
        bill.setDueDate(period.getDueDate());
        bill.setUsageGallons(totalUsage);
        bill.setBaseCharge(charges.get("BASE_FEE"));
        bill.setUsageCharge(charges.get("USAGE_CHARGE"));
        bill.setSurcharges(charges.getOrDefault("SURCHARGES", BigDecimal.ZERO));
        bill.setDiscounts(charges.getOrDefault("DISCOUNTS", BigDecimal.ZERO));
        bill.setTotalAmount(charges.get("TOTAL"));
        bill.setStatus("DRAFT");

        return billRepository.save(bill);
    }

    @Transactional
    public void issueBills(String billingPeriodId) {
        List<Bill> bills = billRepository.findByBillingPeriodIdAndStatus(billingPeriodId, "DRAFT");
        
        for (Bill bill : bills) {
            bill.setStatus("ISSUED");
            billRepository.save(bill);
        }
    }

    @Transactional
    public void sendBill(String billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        emailService.sendBillEmail(bill);
        
        bill.setStatus("SENT");
        bill.setSentDate(LocalDate.now());
        billRepository.save(bill);
    }
}