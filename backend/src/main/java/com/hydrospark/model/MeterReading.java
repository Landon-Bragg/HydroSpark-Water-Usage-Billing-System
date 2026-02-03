package com.hydrospark.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "meter_readings")
@Data
public class MeterReading {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "meter_id", nullable = false)
    private Meter meter;
    
    @Column(nullable = false)
    private LocalDate readDate;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal readingValue;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal usageGallons;
    
    @Enumerated(EnumType.STRING)
    private ReadingType readingType = ReadingType.ACTUAL;
    
    private String notes;
    
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
