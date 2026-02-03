package com.hydrospark.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "seasonal_rates")
@Data
public class SeasonalRate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "rate_plan_id", nullable = false)
    private RatePlan ratePlan;
    
    @Column(nullable = false)
    private String seasonName;
    
    @Column(nullable = false)
    private Integer startMonth;
    
    @Column(nullable = false)
    private Integer endMonth;
    
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal multiplier;
    
    private String description;
}
