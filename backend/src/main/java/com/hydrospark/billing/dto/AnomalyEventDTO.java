package com.hydrospark.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for anomaly events.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalyEventDTO {
    private String id;

    private String customerId;
    private String meterId;

    private LocalDate eventDate;

    /** String representation of AnomalyEvent.EventType enum. */
    private String eventType;

    /** String representation of AnomalyEvent.Severity enum. */
    private String severity;

    private String description;

    /** String representation of AnomalyEvent.Status enum. */
    private String status;

    private String createdBy;
    private String resolvedBy;
    private String resolutionNote;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
