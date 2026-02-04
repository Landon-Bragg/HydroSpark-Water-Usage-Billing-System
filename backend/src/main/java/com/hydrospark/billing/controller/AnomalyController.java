package com.hydrospark.billing.controller;

import com.hydrospark.billing.dto.AnomalyEventDTO;
import com.hydrospark.billing.model.AnomalyEvent;
import com.hydrospark.billing.model.Meter;
import com.hydrospark.billing.repository.AnomalyEventRepository;
import com.hydrospark.billing.repository.MeterRepository;
import com.hydrospark.billing.service.AnomalyDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/anomalies")
@RequiredArgsConstructor
public class AnomalyController {

    private final AnomalyEventRepository anomalyEventRepository;
    private final MeterRepository meterRepository;
    private final AnomalyDetectionService anomalyDetectionService;

    @GetMapping
    public ResponseEntity<List<AnomalyEventDTO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity
    ) {
        Stream<AnomalyEvent> stream = anomalyEventRepository.findAll().stream();

        if (status != null && !status.isBlank()) {
            stream = stream.filter(a -> a.getStatus() != null && a.getStatus().name().equalsIgnoreCase(status));
        }
        if (severity != null && !severity.isBlank()) {
            stream = stream.filter(a -> a.getSeverity() != null && a.getSeverity().name().equalsIgnoreCase(severity));
        }

        List<AnomalyEventDTO> out = stream
                .sorted(Comparator.comparing(AnomalyEvent::getEventDate).reversed())
                .map(AnomalyController::toDto)
                .toList();

        return ResponseEntity.ok(out);
    }

    @GetMapping("/{anomalyId}")
    public ResponseEntity<AnomalyEventDTO> get(@PathVariable String anomalyId) {
        AnomalyEvent a = anomalyEventRepository.findById(anomalyId)
                .orElseThrow(() -> new RuntimeException("Anomaly not found: " + anomalyId));
        return ResponseEntity.ok(toDto(a));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AnomalyEventDTO>> byCustomer(@PathVariable String customerId) {
        List<AnomalyEventDTO> out = anomalyEventRepository.findByCustomerId(customerId).stream()
                .sorted(Comparator.comparing(AnomalyEvent::getEventDate).reversed())
                .map(AnomalyController::toDto)
                .toList();
        return ResponseEntity.ok(out);
    }

    @GetMapping("/meter/{meterId}")
    public ResponseEntity<List<AnomalyEventDTO>> byMeter(@PathVariable String meterId) {
        List<AnomalyEventDTO> out = anomalyEventRepository.findByMeterId(meterId).stream()
                .sorted(Comparator.comparing(AnomalyEvent::getEventDate).reversed())
                .map(AnomalyController::toDto)
                .toList();
        return ResponseEntity.ok(out);
    }

    /**
     * Run anomaly detection now.
     * - If meterId provided: runs detection for that single meter
     * - If not: runs detection for all active meters (same as scheduled job)
     */
    @PostMapping("/run")
    public ResponseEntity<?> runNow(@RequestParam(required = false) String meterId) {
        if (meterId == null || meterId.isBlank()) {
            anomalyDetectionService.runDailyAnomalyDetection();
            return ResponseEntity.ok().build();
        }

        Meter meter = meterRepository.findById(meterId)
                .orElseThrow(() -> new RuntimeException("Meter not found: " + meterId));

        int detected = anomalyDetectionService.detectAnomaliesForMeter(meter);
        return ResponseEntity.ok(detected);
    }

    @PostMapping("/{anomalyId}/resolve")
    public ResponseEntity<?> resolve(
            @PathVariable String anomalyId,
            @RequestParam String userId,
            @RequestParam(required = false, defaultValue = "") String note
    ) {
        anomalyDetectionService.resolveAnomaly(anomalyId, userId, note);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{anomalyId}/dismiss")
    public ResponseEntity<?> dismiss(
            @PathVariable String anomalyId,
            @RequestParam String userId,
            @RequestParam(required = false, defaultValue = "") String reason
    ) {
        anomalyDetectionService.dismissAnomaly(anomalyId, userId, reason);
        return ResponseEntity.ok().build();
    }

    private static AnomalyEventDTO toDto(AnomalyEvent a) {
        AnomalyEventDTO dto = new AnomalyEventDTO();
        dto.setId(a.getId());
        dto.setCustomerId(a.getCustomerId());
        dto.setMeterId(a.getMeterId());
        dto.setEventDate(a.getEventDate());
        dto.setEventType(a.getEventType() != null ? a.getEventType().name() : null);
        dto.setSeverity(a.getSeverity() != null ? a.getSeverity().name() : null);
        dto.setStatus(a.getStatus() != null ? a.getStatus().name() : null);
        dto.setDescription(a.getDescription());
        dto.setResolvedBy(a.getResolvedBy());
        dto.setResolutionNote(a.getResolutionNote());
        return dto;
    }
}
