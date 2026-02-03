package com.hydrospark.billing.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "usage_forecasts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageForecast {
    
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(columnDefinition = "CHAR(36)")
    private String id;
    
    @Column(name = "customer_id", nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;
    
    @Column(name = "meter_id", nullable = false, columnDefinition = "CHAR(36)")
    private String meterId;
    
    @Column(name = "billing_cycle_number", nullable = false)
    private Integer billingCycleNumber;
    
    @Column(name = "target_period_start", nullable = false)
    private LocalDate targetPeriodStart;
    
    @Column(name = "target_period_end", nullable = false)
    private LocalDate targetPeriodEnd;
    
    @Column(name = "predicted_total_ccf", nullable = false, precision = 10, scale = 2)
    private BigDecimal predictedTotalCcf;
    
    @Column(name = "predicted_total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal predictedTotalAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ForecastMethod method;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_level", nullable = false)
    private ConfidenceLevel confidenceLevel;
    
    @Column(name = "historical_periods_used", nullable = false)
    private Integer historicalPeriodsUsed;
    
    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
    }
    
    public enum ForecastMethod {
        SIMPLE_AVG, SEASONAL_AVG, TREND_BASED
    }
    
    public enum ConfidenceLevel {
        LOW, MEDIUM, HIGH
    }
}
