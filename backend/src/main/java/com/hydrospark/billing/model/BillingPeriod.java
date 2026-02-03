package com.hydrospark.billing.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "billing_periods",
       uniqueConstraints = @UniqueConstraint(columnNames = {"cycle_number", "period_start_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingPeriod {
    
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(columnDefinition = "CHAR(36)")
    private String id;
    
    @Column(name = "cycle_number", nullable = false)
    private Integer cycleNumber;
    
    @Column(name = "period_start_date", nullable = false)
    private LocalDate periodStartDate;
    
    @Column(name = "period_end_date", nullable = false)
    private LocalDate periodEndDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum Status {
        OPEN, CLOSED, BILLED
    }
}
