package com.hydrospark.service;

import com.hydrospark.model.AnomalyEvent;
import com.hydrospark.model.Customer;
import com.hydrospark.model.Meter;
import com.hydrospark.model.MeterReading;
import com.hydrospark.repository.AnomalyEventRepository;
import com.hydrospark.repository.CustomerRepository;
import com.hydrospark.repository.MeterReadingRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnomalyDetectionService {

    private final MeterReadingRepository meterReadingRepository;
    private final AnomalyEventRepository anomalyEventRepository;
    private final CustomerRepository customerRepository;

    @Value("${app.anomaly-detection.spike-threshold:3.0}")
    private double spikeThreshold;

    @Value("${app.anomaly-detection.sustained-high-days:3}")
    private int sustainedHighDays;

    @Value("${app.anomaly-detection.zero-usage-days:7}")
    private int zeroUsageDays;

    @Transactional
    public void runAnomalyDetection() {
        List<Customer> customers = customerRepository.findAllByIsActive(true);
        
        for (Customer customer : customers) {
            detectAnomaliesForCustomer(customer);
        }
    }

    private void detectAnomaliesForCustomer(Customer customer) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(90);

        List<MeterReading> readings = meterReadingRepository
                .findByMeterCustomerIdAndReadingDateBetweenOrderByReadingDate(
                        customer.getId(), startDate, endDate);

        if (readings.size() < 30) {
            return; // Not enough data for analysis
        }

        // Calculate statistics
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (MeterReading reading : readings) {
            stats.addValue(reading.getUsageGallons().doubleValue());
        }

        double mean = stats.getMean();
        double stdDev = stats.getStandardDeviation();

        // Check recent readings for anomalies
        List<MeterReading> recentReadings = readings.subList(
                Math.max(0, readings.size() - 7), readings.size());

        for (MeterReading reading : recentReadings) {
            double usage = reading.getUsageGallons().doubleValue();

            // Spike detection
            if (usage > mean + (spikeThreshold * stdDev) && usage > 0) {
                createAnomalyEvent(
                        customer,
                        reading.getMeter(),
                        "SPIKE",
                        "HIGH",
                        reading.getReadingDate(),
                        String.format("Usage spike detected: %.0f gallons (%.1fx normal)",
                                usage, usage / mean),
                        BigDecimal.valueOf(usage),
                        BigDecimal.valueOf(mean)
                );
            }

            // Zero usage detection
            if (usage == 0) {
                long consecutiveZeros = countConsecutiveZeros(reading.getMeter(), reading.getReadingDate());
                if (consecutiveZeros >= zeroUsageDays) {
                    createAnomalyEvent(
                            customer,
                            reading.getMeter(),
                            "ZERO_USAGE",
                            "MEDIUM",
                            reading.getReadingDate(),
                            String.format("Zero usage for %d consecutive days", consecutiveZeros),
                            BigDecimal.ZERO,
                            BigDecimal.valueOf(mean)
                    );
                }
            }

            // Sustained high usage
            if (usage > mean * 2) {
                long consecutiveHigh = countConsecutiveHighUsage(reading.getMeter(), reading.getReadingDate(), mean * 2);
                if (consecutiveHigh >= sustainedHighDays) {
                    createAnomalyEvent(
                            customer,
                            reading.getMeter(),
                            "SUSTAINED_HIGH",
                            "HIGH",
                            reading.getReadingDate(),
                            String.format("Sustained high usage for %d days (avg: %.0f gallons)",
                                    consecutiveHigh, usage),
                            BigDecimal.valueOf(usage),
                            BigDecimal.valueOf(mean)
                    );
                }
            }
        }
    }

    private void createAnomalyEvent(Customer customer, Meter meter, String type, String severity,
                                  LocalDate detectedDate, String description,
                                  BigDecimal usageValue, BigDecimal expectedValue) {
        // Check if anomaly already exists
        boolean exists = anomalyEventRepository.existsByMeterIdAndAnomalyTypeAndDetectedDateAndStatus(
                meter.getId(), type, detectedDate, "OPEN");

        if (!exists) {
            AnomalyEvent anomaly = new AnomalyEvent();
            anomaly.setId(UUID.randomUUID().toString());
            anomaly.setCustomer(customer);
            anomaly.setMeter(meter);
            anomaly.setAnomalyType(type);
            anomaly.setSeverity(severity);
            anomaly.setDetectedDate(detectedDate);
            anomaly.setDescription(description);
            anomaly.setUsageValue(usageValue);
            anomaly.setExpectedValue(expectedValue);
            anomaly.setStatus("OPEN");

            if (usageValue.compareTo(BigDecimal.ZERO) > 0 && expectedValue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal deviation = usageValue.subtract(expectedValue)
                        .divide(expectedValue, 2, BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                anomaly.setDeviationPercentage(deviation);
            }

            anomalyEventRepository.save(anomaly);
        }
    }

    private long countConsecutiveZeros(Meter meter, LocalDate endDate) {
        LocalDate checkDate = endDate;
        long count = 0;

        for (int i = 0; i < 30; i++) {
            MeterReading reading = meterReadingRepository
                    .findByMeterIdAndReadingDate(meter.getId(), checkDate)
                    .orElse(null);

            if (reading == null || reading.getUsageGallons().compareTo(BigDecimal.ZERO) > 0) {
                break;
            }

            count++;
            checkDate = checkDate.minusDays(1);
        }

        return count;
    }

    private long countConsecutiveHighUsage(Meter meter, LocalDate endDate, double threshold) {
        LocalDate checkDate = endDate;
        long count = 0;

        for (int i = 0; i < 30; i++) {
            MeterReading reading = meterReadingRepository
                    .findByMeterIdAndReadingDate(meter.getId(), checkDate)
                    .orElse(null);

            if (reading == null || reading.getUsageGallons().doubleValue() <= threshold) {
                break;
            }

            count++;
            checkDate = checkDate.minusDays(1);
        }

        return count;
    }
}