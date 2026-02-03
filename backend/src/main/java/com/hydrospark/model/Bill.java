package com.hydrospark.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "bills")
@Data
public class Bill {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @Column(nullable = false, unique = true)
    private String billNumber;
    
    @Column(nullable = false)
    private LocalDate billingPeriodStart;
    
    @Column(nullable = false)
    private LocalDate billingPeriodEnd;
    
    @Column(nullable = false)
    private LocalDate dueDate;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal usageGallons;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal baseCharge;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal usageCharge;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal surcharges;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal taxes;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    private BillStatus status = BillStatus.PENDING;
    
    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL)
    private List<BillLineItem> lineItems;
    
    private LocalDateTime issuedAt;
    
    private LocalDateTime sentAt;
    
    private LocalDateTime paidAt;
    
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
