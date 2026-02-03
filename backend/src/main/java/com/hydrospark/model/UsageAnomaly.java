package com.hydrospark.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "usage_anomalies")
@Data
public class UsageAnomaly {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @ManyToOne
    @JoinColumn(name = "meter_id", nullable = false)
    private Meter meter;
    
    @Column(nullable = false)
    private LocalDate detectedDate;
    
    @Enumerated(EnumType.STRING)
    private AnomalyType anomalyType;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal expectedUsage;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal actualUsage;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal variance;
    
    @Enumerated(EnumType.STRING)
    private AnomalySeverity severity;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String resolution;
    
    @Enumerated(EnumType.STRING)
    private AnomalyStatus status = AnomalyStatus.DETECTED;
    
    private LocalDateTime resolvedAt;
    
    private String resolvedBy;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
