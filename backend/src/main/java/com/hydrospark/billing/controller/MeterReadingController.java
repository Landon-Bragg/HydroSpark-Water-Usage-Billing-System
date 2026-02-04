package com.hydrospark.billing.controller;

import com.hydrospark.billing.dto.MeterReadingDTO;
import com.hydrospark.billing.model.Meter;
import com.hydrospark.billing.model.MeterReading;
import com.hydrospark.billing.repository.MeterReadingRepository;
import com.hydrospark.billing.repository.MeterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for meter reading operations
 */
@RestController
@RequestMapping("/api/meter-readings")
@RequiredArgsConstructor
public class MeterReadingController {

    private final MeterReadingRepository meterReadingRepository;
    private final MeterRepository meterRepository;

    /**
     * Get readings for a specific meter within a date range
     */
    @GetMapping("/meter/{meterId}")
    public ResponseEntity<List<MeterReadingDTO>> getReadingsByMeter(
            @PathVariable String meterId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<MeterReading> readings = meterReadingRepository
                .findByMeterIdAndReadingDateBetweenOrderByReadingDateAsc(meterId, startDate, endDate);

        List<MeterReadingDTO> dtos = readings.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get readings for all meters belonging to a customer
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<MeterReadingDTO>> getReadingsByCustomer(
            @PathVariable String customerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // Get all meters for the customer
        List<Meter> customerMeters = meterRepository.findByCustomerId(customerId);

        // Get readings for all meters
        List<MeterReading> allReadings = customerMeters.stream()
                .flatMap(meter -> meterReadingRepository
                        .findByMeterIdAndReadingDateBetweenOrderByReadingDateAsc(
                                meter.getId(), startDate, endDate)
                        .stream())
                .collect(Collectors.toList());

        List<MeterReadingDTO> dtos = allReadings.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get latest reading for a meter
     */
    @GetMapping("/meter/{meterId}/latest")
    public ResponseEntity<MeterReadingDTO> getLatestReading(@PathVariable String meterId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(90); // Look back 90 days

        List<MeterReading> readings = meterReadingRepository
                .findByMeterIdAndReadingDateBetweenOrderByReadingDateAsc(meterId, startDate, endDate);

        if (readings.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MeterReading latest = readings.get(readings.size() - 1);
        return ResponseEntity.ok(toDTO(latest));
    }

    /**
     * Record a new meter reading (manual entry)
     */
    @PostMapping
    public ResponseEntity<MeterReadingDTO> recordReading(@RequestBody RecordReadingRequest request) {
        // Check if reading already exists
        var existing = meterReadingRepository.findByMeterIdAndReadingDate(
                request.meterId(), request.readingDate());

        if (existing.isPresent()) {
            throw new RuntimeException("Reading already exists for this meter and date");
        }

        MeterReading reading = MeterReading.builder()
                .meterId(request.meterId())
                .readingDate(request.readingDate())
                .usageCcf(request.usageCcf())
                .source(MeterReading.ReadingSource.MANUAL)
                .build();

        reading = meterReadingRepository.save(reading);

        return ResponseEntity.ok(toDTO(reading));
    }

    /**
     * Convert entity to DTO
     */
    private MeterReadingDTO toDTO(MeterReading reading) {
        return MeterReadingDTO.builder()
                .id(reading.getId())
                .meterId(reading.getMeterId())
                .readingDate(reading.getReadingDate())
                .usageCcf(reading.getUsageCcf())
                .source(reading.getSource() != null ? reading.getSource().name() : null)
                .ingestedAt(reading.getIngestedAt())
                .build();
    }

    /**
     * Request record for manual reading entry
     */
    public record RecordReadingRequest(
            String meterId,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate readingDate,
            java.math.BigDecimal usageCcf
    ) {}
}
