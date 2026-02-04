package com.hydrospark.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for meter readings.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeterReadingDTO {
    private String id;

    private String meterId;
    private LocalDate readingDate;

    /** Usage in CCF (hundred cubic feet) - matches MeterReading.usageCcf */
    private BigDecimal usageCcf;

    /** String representation of MeterReading.ReadingSource enum. */
    private String source;

    private LocalDateTime ingestedAt;
}
