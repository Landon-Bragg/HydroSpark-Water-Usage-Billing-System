package com.hydrospark.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "rate_tiers")
@Data
public class RateTier {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "rate_plan_id", nullable = false)
    private RatePlan ratePlan;
    
    @Column(nullable = false)
    private Integer tierLevel;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal minUsage;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal maxUsage;
    
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal ratePerGallon;
    
    private String description;
}
