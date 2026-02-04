package com.hydrospark.billing.service;

import com.hydrospark.billing.model.AnomalyEvent;
import com.hydrospark.billing.model.Customer;
import com.hydrospark.billing.model.Meter;
import com.hydrospark.billing.model.MeterReading;
import com.hydrospark.billing.repository.AnomalyEventRepository;
import com.hydrospark.billing.repository.CustomerRepository;
import com.hydrospark.billing.repository.MeterReadingRepository;
import com.hydrospark.billing.repository.MeterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionService {

    private final MeterReadingRepository meterReadingRepository;
    private final MeterRepository meterRepository;
    private final CustomerRepository customerRepository;
    private final AnomalyEventRepository anomalyEventRepository;
    private final EmailService emailService;

    // Configurable thresholds
    private static final double SPIKE_THRESHOLD_SIGMA = 3.0;  // 3 standard deviations
    private static final int SUSTAINED_HIGH_DAYS = 3;
    private static final int ZERO_USAGE_DAYS = 7;
    private static final int DATA_GAP_DAYS = 3;
    private static final double SUSTAINED_HIGH_MULTIPLIER = 2.0;

    /**
     * Run anomaly detection for all active meters
     * Scheduled to run daily at 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void runDailyAnomalyDetection() {
        log.info("Starting daily anomaly detection...");

        List<Meter> activeMeters = meterRepository.findByStatus(Meter.Status.ACTIVE);
        int anomaliesDetected = 0;

        for (Meter meter : activeMeters) {
            try {
                int count = detectAnomaliesForMeter(meter);
                anomaliesDetected += count;
            } catch (Exception e) {
                log.error("Error detecting anomalies for meter {}: {}", meter.getId(), e.getMessage());
            }
        }

        log.info("Anomaly detection complete. Detected {} new anomalies.", anomaliesDetected);
    }

    /**
     * Detect anomalies for a specific meter
     */
    @Transactional
    public int detectAnomaliesForMeter(Meter meter) {
        log.debug("Checking meter {} for anomalies", meter.getExternalLocationId());

        // Get historical readings (last 90 days)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(90);

        List<MeterReading> readings = meterReadingRepository
                .findByMeterIdAndReadingDateBetweenOrderByReadingDateAsc(
                        meter.getId(), startDate, endDate);

        if (readings.size() < 30) {
            log.debug("Insufficient data for meter {} ({} readings)", meter.getId(), readings.size());
            return 0;
        }

        int anomaliesDetected = 0;

        // Calculate statistics
        UsageStatistics stats = calculateStatistics(readings);

        // Get recent readings (last 7 days)
        LocalDate recentStartDate = endDate.minusDays(7);
        List<MeterReading> recentReadings = readings.stream()
                .filter(r -> !r.getReadingDate().isBefore(recentStartDate))
                .toList();

        // Check each recent reading for anomalies
        for (MeterReading reading : recentReadings) {

            // Spike
            if (detectSpike(reading, stats)) {
                createAnomalyEvent(meter, reading, AnomalyEvent.EventType.SPIKE,
                        AnomalyEvent.Severity.HIGH, stats);
                anomaliesDetected++;
            }

            // Zero usage
            if (detectZeroUsage(meter, reading.getReadingDate())) {
                createAnomalyEvent(meter, reading, AnomalyEvent.EventType.ZERO_USAGE,
                        AnomalyEvent.Severity.MEDIUM, stats);
                anomaliesDetected++;
            }

            // Sustained high usage
            if (detectSustainedHighUsage(meter, reading.getReadingDate(), stats)) {
                createAnomalyEvent(meter, reading, AnomalyEvent.EventType.SUSTAINED_HIGH,
                        AnomalyEvent.Severity.HIGH, stats);
                anomaliesDetected++;
            }
        }

        // Data gaps
        if (detectDataGap(readings, endDate)) {
            createDataGapAnomaly(meter, endDate);
            anomaliesDetected++;
        }

        return anomaliesDetected;
    }

    private boolean detectSpike(MeterReading reading, UsageStatistics stats) {
        if (reading.getUsageCcf().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        double usage = reading.getUsageCcf().doubleValue();
        double threshold = stats.mean + (SPIKE_THRESHOLD_SIGMA * stats.stdDev);

        return usage > threshold && usage > stats.mean * 1.5;
    }

    private boolean detectZeroUsage(Meter meter, LocalDate date) {
        LocalDate startDate = date.minusDays(ZERO_USAGE_DAYS - 1);

        List<MeterReading> zeroReadings = meterReadingRepository
                .findByMeterIdAndReadingDateBetweenOrderByReadingDateAsc(meter.getId(), startDate, date);

        if (zeroReadings.size() < ZERO_USAGE_DAYS) {
            return false;
        }

        return zeroReadings.stream()
                .allMatch(r -> r.getUsageCcf().compareTo(BigDecimal.ZERO) == 0);
    }

    private boolean detectSustainedHighUsage(Meter meter, LocalDate date, UsageStatistics stats) {
        LocalDate startDate = date.minusDays(SUSTAINED_HIGH_DAYS - 1);

        List<MeterReading> highReadings = meterReadingRepository
                .findByMeterIdAndReadingDateBetweenOrderByReadingDateAsc(meter.getId(), startDate, date);

        if (highReadings.size() < SUSTAINED_HIGH_DAYS) {
            return false;
        }

        double threshold = stats.mean * SUSTAINED_HIGH_MULTIPLIER;

        return highReadings.stream()
                .allMatch(r -> r.getUsageCcf().doubleValue() > threshold);
    }

    private boolean detectDataGap(List<MeterReading> readings, LocalDate endDate) {
        if (readings.isEmpty()) return false;

        LocalDate lastReading = readings.get(readings.size() - 1).getReadingDate();
        long daysSinceLastReading = java.time.temporal.ChronoUnit.DAYS.between(lastReading, endDate);

        return daysSinceLastReading >= DATA_GAP_DAYS;
    }

    private void createAnomalyEvent(Meter meter, MeterReading reading,
                                   AnomalyEvent.EventType eventType,
                                   AnomalyEvent.Severity severity,
                                   UsageStatistics stats) {

        // Avoid duplicates: same meter/date/type while OPEN
        Optional<AnomalyEvent> existing = anomalyEventRepository.findByMeterId(meter.getId()).stream()
                .filter(a -> a.getEventDate().equals(reading.getReadingDate())
                        && a.getEventType() == eventType
                        && a.getStatus() == AnomalyEvent.Status.OPEN)
                .findFirst();

        if (existing.isPresent()) {
            log.debug("Anomaly already exists for meter {} on {}", meter.getId(), reading.getReadingDate());
            return;
        }

        Customer customer = customerRepository.findById(meter.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        String description = buildDescription(eventType, reading, stats);

        AnomalyEvent anomaly = AnomalyEvent.builder()
                .customerId(customer.getId())
                .meterId(meter.getId())
                .eventDate(reading.getReadingDate())
                .eventType(eventType)
                .severity(severity)
                .description(description)
                .status(AnomalyEvent.Status.OPEN)
                .build();

        anomalyEventRepository.save(anomaly);

        log.info("Anomaly detected: {} for meter {} on {}", eventType, meter.getExternalLocationId(),
                reading.getReadingDate());

        if (customer.getEmail() != null && !customer.getEmail().isBlank()) {
            emailService.sendAnomalyAlertEmail(customer.getEmail(), customer.getName(), description);
        }
    }

    private void createDataGapAnomaly(Meter meter, LocalDate date) {
        Customer customer = customerRepository.findById(meter.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        String description = String.format("No meter readings received for %d or more consecutive days", DATA_GAP_DAYS);

        AnomalyEvent anomaly = AnomalyEvent.builder()
                .customerId(customer.getId())
                .meterId(meter.getId())
                .eventDate(date)
                .eventType(AnomalyEvent.EventType.DATA_GAP)
                .severity(AnomalyEvent.Severity.MEDIUM)
                .description(description)
                .status(AnomalyEvent.Status.OPEN)
                .build();

        anomalyEventRepository.save(anomaly);

        log.warn("Data gap detected for meter {}", meter.getExternalLocationId());
    }

    private String buildDescription(AnomalyEvent.EventType eventType, MeterReading reading, UsageStatistics stats) {
        double usage = reading.getUsageCcf().doubleValue();

        return switch (eventType) {
            case SPIKE -> String.format(
                    "Usage spike detected: %.1f CCF (%.1fx normal usage). Average usage is %.1f CCF.",
                    usage, usage / stats.mean, stats.mean);

            case SUSTAINED_HIGH -> String.format(
                    "Sustained high usage detected over %d days: %.1f CCF/day (%.1fx normal). This may indicate a leak.",
                    SUSTAINED_HIGH_DAYS, usage, usage / stats.mean);

            case ZERO_USAGE -> String.format(
                    "Zero usage detected for %d consecutive days. This may indicate a meter issue or property vacancy.",
                    ZERO_USAGE_DAYS);

            default -> "Unusual usage pattern detected";
        };
    }

    private UsageStatistics calculateStatistics(List<MeterReading> readings) {
        if (readings.isEmpty()) return new UsageStatistics(0, 0);

        double sum = readings.stream()
                .mapToDouble(r -> r.getUsageCcf().doubleValue())
                .sum();
        double mean = sum / readings.size();

        double variance = readings.stream()
                .mapToDouble(r -> {
                    double diff = r.getUsageCcf().doubleValue() - mean;
                    return diff * diff;
                })
                .sum() / readings.size();

        return new UsageStatistics(mean, Math.sqrt(variance));
    }

    /**
     * Resolve an anomaly event
     */
    @Transactional
    public void resolveAnomaly(String anomalyId, String userId, String resolutionNote) {
        AnomalyEvent anomaly = anomalyEventRepository.findById(anomalyId)
                .orElseThrow(() -> new RuntimeException("Anomaly not found"));

        anomaly.setStatus(AnomalyEvent.Status.RESOLVED);
        anomaly.setResolvedBy(userId);
        anomaly.setResolutionNote(resolutionNote);

        anomalyEventRepository.save(anomaly);

        log.info("Anomaly {} resolved by user {}", anomalyId, userId);
    }

    /**
     * Dismiss an anomaly as false positive
     */
    @Transactional
    public void dismissAnomaly(String anomalyId, String userId, String reason) {
        AnomalyEvent anomaly = anomalyEventRepository.findById(anomalyId)
                .orElseThrow(() -> new RuntimeException("Anomaly not found"));

        anomaly.setStatus(AnomalyEvent.Status.DISMISSED);
        anomaly.setResolvedBy(userId);
        anomaly.setResolutionNote("Dismissed: " + reason);

        anomalyEventRepository.save(anomaly);

        log.info("Anomaly {} dismissed by user {}", anomalyId, userId);
    }

    private static class UsageStatistics {
        final double mean;
        final double stdDev;

        UsageStatistics(double mean, double stdDev) {
            this.mean = mean;
            this.stdDev = stdDev;
        }
    }
}
