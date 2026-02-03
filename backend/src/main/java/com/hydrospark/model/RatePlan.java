package com.hydrospark.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "rate_plans")
@Data
public class RatePlan {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    @Column(nullable = false)
    private LocalDate effectiveDate;
    
    private LocalDate endDate;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal baseCharge = BigDecimal.ZERO;
    
    private boolean isActive = true;
    
    @OneToMany(mappedBy = "ratePlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RateTier> rateTiers;
    
    @OneToMany(mappedBy = "ratePlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SeasonalRate> seasonalRates;
    
    @OneToMany(mappedBy = "ratePlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Surcharge> surcharges;
    
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
