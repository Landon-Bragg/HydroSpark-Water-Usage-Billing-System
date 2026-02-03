package com.hydrospark.billing.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "anomaly_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalyEvent {
    
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(columnDefinition = "CHAR(36)")
    private String id;
    
    @Column(name = "customer_id", nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;
    
    @Column(name = "meter_id", nullable = false, columnDefinition = "CHAR(36)")
    private String meterId;
    
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;
    
    @Column(name = "created_by", columnDefinition = "CHAR(36)")
    private String createdBy;
    
    @Column(name = "resolved_by", columnDefinition = "CHAR(36)")
    private String resolvedBy;
    
    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
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
    
    public enum EventType {
        SPIKE, SUSTAINED_HIGH, ZERO_USAGE, NEGATIVE_USAGE, DATA_GAP
    }
    
    public enum Severity {
        LOW, MEDIUM, HIGH
    }
    
    public enum Status {
        OPEN, INVESTIGATING, RESOLVED, DISMISSED
    }
}
