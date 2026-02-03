package com.hydrospark.billing.service;

import com.hydrospark.billing.model.*;
import com.hydrospark.billing.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForecastService {

    private final MeterReadingRepository meterReadingRepository;
    private final MeterRepository meterRepository;
    private final CustomerRepository customerRepository;
    private final RatePlanRepository ratePlanRepository;
    private final UsageForecastRepository usageForecastRepository;
    private final RateEngineService rateEngineService;

    /**
     * Generate usage and cost forecast for a customer's next billing period
     */
    @Transactional
    public UsageForecast generateForecast(String customerId) {
        log.info("Generating forecast for customer: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // Get customer's meter
        List<Meter> meters = meterRepository.findByCustomerId(customerId);
        if (meters.isEmpty()) {
            throw new RuntimeException("No meters found for customer");
        }
        Meter meter = meters.get(0); // Use first meter

        // Determine forecast period (next month)
        LocalDate today = LocalDate.now();
        LocalDate nextMonthStart = today.withDayOfMonth(1).plusMonths(1);
        LocalDate nextMonthEnd = nextMonthStart.with(TemporalAdjusters.lastDayOfMonth());

        // Get historical data for analysis
        LocalDate historicalStart = today.minusMonths(12); // Last 12 months
        List<MeterReading> historicalReadings = meterReadingRepository
                .findByMeterIdAndReadingDateBetweenOrderByReadingDateAsc(
                        meter.getId(), historicalStart, today);

        if (historicalReadings.size() < 30) {
            log.warn("Insufficient historical data for forecast (only {} readings)", historicalReadings.size());
            // Return low confidence forecast
            return createLowConfidenceForecast(customer, meter, nextMonthStart, nextMonthEnd);
        }

        // Calculate predicted usage
        ForecastResult result = calculatePredictedUsage(historicalReadings, nextMonthStart);

        // Get active rate plan
        RatePlan ratePlan = ratePlanRepository
                .findActiveRatePlanForCustomerType(
                        customer.getCustomerType() == Customer.CustomerType.RESIDENTIAL 
                                ? RatePlan.CustomerTypeScope.RESIDENTIAL 
                                : RatePlan.CustomerTypeScope.COMMERCIAL,
                        nextMonthStart)
                .orElseThrow(() -> new RuntimeException("No active rate plan found"));

        // Calculate predicted cost
        RateEngineService.ChargeBreakdown charges = rateEngineService.calculateCharges(
                result.predictedUsageCcf, ratePlan, nextMonthStart);

        // Create forecast record
        UsageForecast forecast = UsageForecast.builder()
                .customerId(customerId)
                .meterId(meter.getId())
                .billingCycleNumber(customer.getBillingCycleNumber())
                .targetPeriodStart(nextMonthStart)
                .targetPeriodEnd(nextMonthEnd)
                .predictedTotalCcf(result.predictedUsageCcf)
                .predictedTotalAmount(charges.getTotalAmount())
                .method(result.method)
                .confidenceLevel(result.confidence)
                .historicalPeriodsUsed(result.periodsUsed)
                .build();

        // Save forecast
        forecast = usageForecastRepository.save(forecast);

        log.info("Forecast generated: {} CCF, ${}, confidence: {}", 
                result.predictedUsageCcf, charges.getTotalAmount(), result.confidence);

        return forecast;
    }

    /**
     * Get the latest forecast for a customer
     */
    public UsageForecast getLatestForecast(String customerId) {
        LocalDate today = LocalDate.now();
        return usageForecastRepository.findLatestForecastForCustomerAndDate(customerId, today)
                .orElse(null);
    }

    private ForecastResult calculatePredictedUsage(List<MeterReading> historicalReadings, LocalDate targetMonth) {
        ForecastResult result = new ForecastResult();

        // Get same month from previous year (seasonal pattern)
        int targetMonthValue = targetMonth.getMonthValue();
        BigDecimal sameMonthLastYear = getSameMonthUsage(historicalReadings, targetMonthValue, 1);
        BigDecimal sameMonthTwoYearsAgo = getSameMonthUsage(historicalReadings, targetMonthValue, 2);

        // Get recent average (last 3 months)
        BigDecimal recentAverage = getRecentAverageUsage(historicalReadings, 3);

        // Get overall average (last 12 months)
        BigDecimal overallAverage = getRecentAverageUsage(historicalReadings, 12);

        // Determine best method and calculate prediction
        if (sameMonthLastYear != null && sameMonthTwoYearsAgo != null) {
            // Use seasonal average with trend adjustment
            BigDecimal seasonalAverage = sameMonthLastYear.add(sameMonthTwoYearsAgo)
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            
            // Calculate trend (is usage increasing or decreasing?)
            BigDecimal trend = sameMonthLastYear.subtract(sameMonthTwoYearsAgo);
            
            result.predictedUsageCcf = seasonalAverage.add(trend.multiply(BigDecimal.valueOf(0.5)));
            result.method = UsageForecast.ForecastMethod.SEASONAL_AVG;
            result.confidence = UsageForecast.ConfidenceLevel.HIGH;
            result.periodsUsed = 2;

        } else if (sameMonthLastYear != null) {
            // Use last year's same month
            result.predictedUsageCcf = sameMonthLastYear;
            result.method = UsageForecast.ForecastMethod.SEASONAL_AVG;
            result.confidence = UsageForecast.ConfidenceLevel.MEDIUM;
            result.periodsUsed = 1;

        } else if (recentAverage != null) {
            // Use recent average with seasonal adjustment
            double seasonalFactor = getSeasonalFactor(targetMonthValue);
            result.predictedUsageCcf = recentAverage.multiply(BigDecimal.valueOf(seasonalFactor));
            result.method = UsageForecast.ForecastMethod.SIMPLE_AVG;
            result.confidence = UsageForecast.ConfidenceLevel.MEDIUM;
            result.periodsUsed = 3;

        } else {
            // Fall back to overall average
            result.predictedUsageCcf = overallAverage;
            result.method = UsageForecast.ForecastMethod.SIMPLE_AVG;
            result.confidence = UsageForecast.ConfidenceLevel.LOW;
            result.periodsUsed = historicalReadings.size() / 30; // Approximate months
        }

        // Ensure reasonable bounds
        if (result.predictedUsageCcf.compareTo(BigDecimal.ZERO) <= 0) {
            result.predictedUsageCcf = BigDecimal.valueOf(10); // Minimum 10 CCF
            result.confidence = UsageForecast.ConfidenceLevel.LOW;
        }

        return result;
    }

    private BigDecimal getSameMonthUsage(List<MeterReading> readings, int targetMonth, int yearsAgo) {
        LocalDate targetDate = LocalDate.now().minusYears(yearsAgo).withMonth(targetMonth);
        LocalDate monthStart = targetDate.withDayOfMonth(1);
        LocalDate monthEnd = targetDate.with(TemporalAdjusters.lastDayOfMonth());

        return readings.stream()
                .filter(r -> !r.getReadingDate().isBefore(monthStart) && !r.getReadingDate().isAfter(monthEnd))
                .map(MeterReading::getUsageCcf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getRecentAverageUsage(List<MeterReading> readings, int months) {
        LocalDate cutoffDate = LocalDate.now().minusMonths(months);

        List<MeterReading> recentReadings = readings.stream()
                .filter(r -> !r.getReadingDate().isBefore(cutoffDate))
                .toList();

        if (recentReadings.isEmpty()) {
            return null;
        }

        BigDecimal total = recentReadings.stream()
                .map(MeterReading::getUsageCcf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int days = recentReadings.size();
        int expectedDaysInMonth = 30;

        return total.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(expectedDaysInMonth));
    }

    private double getSeasonalFactor(int month) {
        // Simple seasonal factors (can be refined with more data)
        return switch (month) {
            case 6, 7, 8 -> 1.3;  // Summer - higher usage (lawn watering, etc.)
            case 12, 1, 2 -> 0.9;  // Winter - lower usage
            default -> 1.0;        // Spring/Fall - normal
        };
    }

    private UsageForecast createLowConfidenceForecast(Customer customer, Meter meter, 
                                                      LocalDate start, LocalDate end) {
        // Use a conservative estimate
        BigDecimal estimatedUsage = BigDecimal.valueOf(20); // 20 CCF default

        RatePlan ratePlan = ratePlanRepository
                .findActiveRatePlanForCustomerType(
                        customer.getCustomerType() == Customer.CustomerType.RESIDENTIAL 
                                ? RatePlan.CustomerTypeScope.RESIDENTIAL 
                                : RatePlan.CustomerTypeScope.COMMERCIAL,
                        start)
                .orElseThrow(() -> new RuntimeException("No active rate plan found"));

        RateEngineService.ChargeBreakdown charges = rateEngineService.calculateCharges(
                estimatedUsage, ratePlan, start);

        return UsageForecast.builder()
                .customerId(customer.getId())
                .meterId(meter.getId())
                .billingCycleNumber(customer.getBillingCycleNumber())
                .targetPeriodStart(start)
                .targetPeriodEnd(end)
                .predictedTotalCcf(estimatedUsage)
                .predictedTotalAmount(charges.getTotalAmount())
                .method(UsageForecast.ForecastMethod.SIMPLE_AVG)
                .confidenceLevel(UsageForecast.ConfidenceLevel.LOW)
                .historicalPeriodsUsed(0)
                .build();
    }

    private static class ForecastResult {
        BigDecimal predictedUsageCcf;
        UsageForecast.ForecastMethod method;
        UsageForecast.ConfidenceLevel confidence;
        int periodsUsed;
    }
}
