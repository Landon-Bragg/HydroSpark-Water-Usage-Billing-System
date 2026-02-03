package com.hydrospark.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "surcharges")
@Data
public class Surcharge {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "rate_plan_id", nullable = false)
    private RatePlan ratePlan;
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    private SurchargeType type;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    private String description;
    
    private boolean isActive = true;
}
