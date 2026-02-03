package com.hydrospark.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "meters")
@Data
public class Meter {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @Column(nullable = false, unique = true)
    private String meterNumber;
    
    @Column(nullable = false)
    private String location;
    
    @Enumerated(EnumType.STRING)
    private MeterStatus status = MeterStatus.ACTIVE;
    
    private LocalDateTime installedDate;
    
    private LocalDateTime lastReadDate;
    
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
