package com.hydrospark.billing.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bill_line_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillLineItem {
    
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(columnDefinition = "CHAR(36)")
    private String id;
    
    @Column(name = "bill_id", nullable = false, columnDefinition = "CHAR(36)")
    private String billId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "line_type", nullable = false)
    private LineType lineType;
    
    @Column(nullable = false, length = 500)
    private String description;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal quantity;
    
    private String unit;
    
    @Column(name = "unit_rate", precision = 10, scale = 4)
    private BigDecimal unitRate;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "rate_component_id", columnDefinition = "CHAR(36)")
    private String rateComponentId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum LineType {
        USAGE_CHARGE, FEE, SURCHARGE, ADJUSTMENT
    }
}
