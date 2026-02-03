package com.hydrospark.billing.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "meter_readings", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"meter_id", "reading_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeterReading {
    
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(columnDefinition = "CHAR(36)")
    private String id;
    
    @Column(name = "meter_id", nullable = false, columnDefinition = "CHAR(36)")
    private String meterId;
    
    @Column(name = "reading_date", nullable = false)
    private LocalDate readingDate;
    
    @Column(name = "usage_ccf", nullable = false, precision = 10, scale = 2)
    private BigDecimal usageCcf;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReadingSource source;
    
    @Column(name = "ingested_at", nullable = false)
    private LocalDateTime ingestedAt;
    
    @Column(name = "raw_payload_hash")
    private String rawPayloadHash;
    
    @PrePersist
    protected void onCreate() {
        if (ingestedAt == null) {
            ingestedAt = LocalDateTime.now();
        }
    }
    
    public enum ReadingSource {
        IMPORT_XLSX, IMPORT_CSV, API, MANUAL
    }
}
